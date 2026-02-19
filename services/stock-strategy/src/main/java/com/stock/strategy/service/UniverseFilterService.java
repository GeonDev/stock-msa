package com.stock.strategy.service;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.CorpInfoDto;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.common.dto.StockPriceDto;
import com.stock.strategy.client.CorpClient;
import com.stock.strategy.client.FinanceClient;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.CustomFilterCriteria;
import com.stock.strategy.dto.UniverseFilterCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseFilterService {

    private final CorpClient corpClient;
    private final PriceClient priceClient;
    private final FinanceClient financeClient;

    public List<String> filter(LocalDate baseDate, UniverseFilterCriteria criteria) {
        if (criteria == null) {
            return new ArrayList<>();
        }

        try {
            String market = criteria.getMarket() != null ? criteria.getMarket().name() : "KOSPI";
            String dateStr = baseDate.toString();

            // 1. 시장별 종목 전체 조회
            List<CorpInfoDto> corps = corpClient.getCorpsByMarket(market, dateStr);
            
            // "A" 접두사 제거 및 초기 리스트 생성
            List<String> stockCodes = corps.stream()
                    .map(corp -> corp.getStockCode().replace("A", ""))
                    .collect(Collectors.toList());

            log.info("Initial universe size for {}: {}", market, stockCodes.size());

            // 2. 업종 필터링
            if (criteria.getExcludeSectors() != null && !criteria.getExcludeSectors().isEmpty()) {
                Set<String> excludeSectors = Set.copyOf(criteria.getExcludeSectors());
                stockCodes = corps.stream()
                        .filter(corp -> corp.getSector() == null || !excludeSectors.contains(corp.getSector().name())) 
                        .map(corp -> corp.getStockCode().replace("A", ""))
                        .collect(Collectors.toList());
            }
            
            // 3. 주가 및 시가총액/거래량 필터링
            stockCodes = filterByPrice(stockCodes, dateStr, criteria);
            log.info("After Price filtering: {}", stockCodes.size());

            // 4. 상세 지표 필터링 (재무 및 모멘텀)
            if (criteria.getCustomFilter() != null) {
                stockCodes = filterByCustomCriteria(stockCodes, dateStr, criteria.getCustomFilter());
                log.info("After Custom filtering: {}", stockCodes.size());
            }

            return stockCodes;

        } catch (Exception e) {
            log.error("Failed to filter universe", e);
            return new ArrayList<>();
        }
    }

    private List<String> filterByPrice(List<String> stockCodes, String dateStr, UniverseFilterCriteria criteria) {
        if (criteria.getMinMarketCap() == null && criteria.getMaxMarketCap() == null && criteria.getMinTradingVolume() == null) {
            return stockCodes;
        }

        List<StockPriceDto> prices = priceClient.getPricesByDateBatch(stockCodes, dateStr);
        return prices.stream()
                .filter(p -> {
                    if (criteria.getMinMarketCap() != null) {
                        BigDecimal minCap = BigDecimal.valueOf(criteria.getMinMarketCap()).multiply(new BigDecimal("100000000")); // 억 원 단위
                        if (p.getMarketTotalAmt() == null || p.getMarketTotalAmt().compareTo(minCap) < 0) return false;
                    }
                    if (criteria.getMaxMarketCap() != null) {
                        BigDecimal maxCap = BigDecimal.valueOf(criteria.getMaxMarketCap()).multiply(new BigDecimal("100000000"));
                        if (p.getMarketTotalAmt() == null || p.getMarketTotalAmt().compareTo(maxCap) > 0) return false;
                    }
                    if (criteria.getMinTradingVolume() != null) {
                        BigDecimal minVol = BigDecimal.valueOf(criteria.getMinTradingVolume());
                        if (p.getVolume() == null || p.getVolume().compareTo(minVol) < 0) return false;
                    }
                    return true;
                })
                .map(StockPriceDto::getStockCode)
                .collect(Collectors.toList());
    }

    private List<String> filterByCustomCriteria(List<String> stockCodes, String dateStr, CustomFilterCriteria customFilter) {
        if (stockCodes.isEmpty()) return stockCodes;
        
        List<String> filteredCodes = new ArrayList<>(stockCodes);

        // 재무 지표 필터링
        List<CorpFinanceIndicatorDto> financeIndicators = financeClient.getIndicatorsBatch(filteredCodes, dateStr);
        Map<String, CorpFinanceIndicatorDto> financeMap = financeIndicators.stream()
                .collect(Collectors.toMap(CorpFinanceIndicatorDto::getCorpCode, i -> i, (existing, replacement) -> existing));

        filteredCodes = filteredCodes.stream()
                .filter(code -> {
                    CorpFinanceIndicatorDto i = financeMap.get(code);
                    if (i == null) return false; 

                    if (customFilter.getOnlyProfitable() != null && customFilter.getOnlyProfitable()) {
                        if (i.getRoe() == null || i.getRoe().compareTo(BigDecimal.ZERO) <= 0) return false;
                    }
                    if (customFilter.getMinPer() != null && (i.getPer() == null || i.getPer().compareTo(customFilter.getMinPer()) < 0)) return false;
                    if (customFilter.getMaxPer() != null && (i.getPer() == null || i.getPer().compareTo(customFilter.getMaxPer()) > 0)) return false;
                    if (customFilter.getMinPbr() != null && (i.getPbr() == null || i.getPbr().compareTo(customFilter.getMinPbr()) < 0)) return false;
                    if (customFilter.getMaxPbr() != null && (i.getPbr() == null || i.getPbr().compareTo(customFilter.getMaxPbr()) > 0)) return false;
                    if (customFilter.getMinRoe() != null && (i.getRoe() == null || i.getRoe().compareTo(customFilter.getMinRoe()) < 0)) return false;
                    if (customFilter.getMinPsr() != null && (i.getPsr() == null || i.getPsr().compareTo(customFilter.getMinPsr()) < 0)) return false;

                    return true;
                })
                .collect(Collectors.toList());

        // 2. 기술적 지표 및 모멘텀 필터링
        if (isTechnicalFilterRequired(customFilter)) {
            List<StockIndicatorDto> stockIndicators = priceClient.getIndicatorsByDateBatch(filteredCodes, dateStr);
            Map<String, StockIndicatorDto> indicatorMap = stockIndicators.stream()
                    .collect(Collectors.toMap(StockIndicatorDto::getStockCode, i -> i, (existing, replacement) -> existing));
            
            // 이평선 비교를 위해 종가 정보 필요
            Map<String, BigDecimal> priceMap = priceClient.getPricesByDateBatch(filteredCodes, dateStr).stream()
                    .collect(Collectors.toMap(StockPriceDto::getStockCode, StockPriceDto::getEndPrice, (existing, replacement) -> existing));

            filteredCodes = filteredCodes.stream()
                    .filter(code -> {
                        StockIndicatorDto i = indicatorMap.get(code);
                        if (i == null) return false;

                        // 모멘텀 필터
                        if (customFilter.getMinMomentum1m() != null && (i.getMomentum1m() == null || i.getMomentum1m().compareTo(customFilter.getMinMomentum1m()) < 0)) return false;
                        if (customFilter.getMinMomentum3m() != null && (i.getMomentum3m() == null || i.getMomentum3m().compareTo(customFilter.getMinMomentum3m()) < 0)) return false;
                        if (customFilter.getMinMomentum6m() != null && (i.getMomentum6m() == null || i.getMomentum6m().compareTo(customFilter.getMinMomentum6m()) < 0)) return false;

                        // RSI/MACD 필터
                        if (customFilter.getMinRsi14() != null && (i.getRsi14() == null || i.getRsi14().compareTo(customFilter.getMinRsi14()) < 0)) return false;
                        if (customFilter.getMaxRsi14() != null && (i.getRsi14() == null || i.getRsi14().compareTo(customFilter.getMaxRsi14()) > 0)) return false;
                        if (customFilter.getMinMacd() != null && (i.getMacd() == null || i.getMacd().compareTo(customFilter.getMinMacd()) < 0)) return false;
                        if (customFilter.getMinMacdSignal() != null && (i.getMacdSignal() == null || i.getMacdSignal().compareTo(customFilter.getMinMacdSignal()) < 0)) return false;

                        // 이평선 위치 필터
                        BigDecimal currentPrice = priceMap.get(code);
                        if (currentPrice != null) {
                            if (customFilter.getPriceAboveMa20() != null && customFilter.getPriceAboveMa20() && (i.getMa20() == null || currentPrice.compareTo(i.getMa20()) <= 0)) return false;
                            if (customFilter.getPriceAboveMa60() != null && customFilter.getPriceAboveMa60() && (i.getMa60() == null || currentPrice.compareTo(i.getMa60()) <= 0)) return false;
                            if (customFilter.getPriceAboveMa120() != null && customFilter.getPriceAboveMa120() && (i.getMa120() == null || currentPrice.compareTo(i.getMa120()) <= 0)) return false;
                        }

                        return true;
                    })
                    .collect(Collectors.toList());
        }

        return filteredCodes;
    }

    private boolean isTechnicalFilterRequired(CustomFilterCriteria f) {
        return f.getMinMomentum1m() != null || f.getMinMomentum3m() != null || f.getMinMomentum6m() != null ||
               f.getMinRsi14() != null || f.getMaxRsi14() != null || 
               f.getMinMacd() != null || f.getMinMacdSignal() != null ||
               f.getPriceAboveMa20() != null || f.getPriceAboveMa60() != null || f.getPriceAboveMa120() != null;
    }
}
