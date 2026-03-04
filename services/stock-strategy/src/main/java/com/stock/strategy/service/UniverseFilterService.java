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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseFilterService {

    private final CorpClient corpClient;
    private final PriceClient priceClient;
    private final FinanceClient financeClient;

    @Cacheable(value = "universeCache", key = "#baseDate.toString() + ':' + (#criteria != null ? #criteria.hashCode() : 0)")
    public List<String> filter(LocalDate baseDate, UniverseFilterCriteria criteria) {
        log.info("Starting universe filtering for date: {}", baseDate);
        
        final UniverseFilterCriteria finalCriteria;
        if (criteria == null) {
            finalCriteria = new UniverseFilterCriteria();
            finalCriteria.setMarket(com.stock.common.enums.StockMarket.KOSPI);
        } else {
            finalCriteria = criteria;
        }

        String market = finalCriteria.getMarket() != null ? finalCriteria.getMarket().name() : "KOSPI";
        String dateStr = baseDate.toString();

        // 1. 시장 종목 조회 (A005930 형식)
        List<CorpInfoDto> corps = corpClient.getCorpsByMarket(market, dateStr);
        log.info("Initial universe size for {}: {}", market, corps.size());

        if (corps.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> stockCodesWithA = corps.stream()
                .map(CorpInfoDto::getStockCode)
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList());

        // 2. 주가/시총 정보 조회 (005930 형식으로 변환하여 요청)
        List<String> stockCodesWithoutA = stockCodesWithA.stream()
                .map(code -> code.startsWith("A") ? code.substring(1) : code)
                .collect(Collectors.toList());

        List<StockPriceDto> prices = priceClient.getPricesByDateBatch(stockCodesWithoutA, dateStr);
        log.info("Fetched {} prices for date {}", prices.size(), dateStr);
        
        // 다시 A를 붙여서 매핑 (전략 서비스 내부에서는 A 포함 코드 사용)
        Map<String, StockPriceDto> priceMap = prices.stream()
                .collect(Collectors.toMap(
                    p -> "A" + p.getStockCode(), 
                    p -> p, 
                    (p1, p2) -> p1));

        // 3. 필터링 수행
        List<String> filteredCodes = stockCodesWithA.stream()
                .filter(code -> {
                    StockPriceDto price = priceMap.get(code);
                    if (price == null) return false;

                    // 시가총액 필터
                    if (finalCriteria.getMinMarketCap() != null && 
                        (price.getMarketTotalAmt() == null || price.getMarketTotalAmt().compareTo(BigDecimal.valueOf(finalCriteria.getMinMarketCap() * 100000000L)) < 0)) {
                        return false;
                    }
                    if (finalCriteria.getMaxMarketCap() != null && 
                        (price.getMarketTotalAmt() == null || price.getMarketTotalAmt().compareTo(BigDecimal.valueOf(finalCriteria.getMaxMarketCap() * 100000000L)) > 0)) {
                        return false;
                    }

                    // 시가총액 분위수 필터
                    if (finalCriteria.getMinMarketCapPercentile() != null && 
                        (price.getMarketCapPercentile() == null || price.getMarketCapPercentile().doubleValue() < finalCriteria.getMinMarketCapPercentile())) {
                        return false;
                    }
                    if (finalCriteria.getMaxMarketCapPercentile() != null && 
                        (price.getMarketCapPercentile() == null || price.getMarketCapPercentile().doubleValue() > finalCriteria.getMaxMarketCapPercentile())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        log.info("Universe size after price/cap filters: {}", filteredCodes.size());

        // 4. 상세 지표 필터
        if (finalCriteria.getCustomFilter() != null && !filteredCodes.isEmpty()) {
            List<CorpFinanceIndicatorDto> indicators = financeClient.getIndicatorsBatch(filteredCodes, dateStr);
            log.info("Fetched {} finance indicators", indicators.size());
            
            Map<String, CorpFinanceIndicatorDto> indicatorMap = indicators.stream()
                    .collect(Collectors.toMap(CorpFinanceIndicatorDto::getCorpCode, i -> i, (i1, i2) -> i1));

            filteredCodes = filteredCodes.stream()
                    .filter(code -> {
                        CorpFinanceIndicatorDto indicator = indicatorMap.get(code);
                        if (indicator == null) return false;
                        return applyCustomFilter(indicator, finalCriteria.getCustomFilter());
                    })
                    .collect(Collectors.toList());
            
            log.info("Universe size after custom filters: {}", filteredCodes.size());
        }

        return filteredCodes;
    }

    private boolean applyCustomFilter(CorpFinanceIndicatorDto indicator, CustomFilterCriteria f) {
        if (f == null) return true;

        if (f.getMinPer() != null && (indicator.getPer() == null || indicator.getPer().compareTo(f.getMinPer()) < 0)) return false;
        if (f.getMaxPer() != null && (indicator.getPer() == null || indicator.getPer().compareTo(f.getMaxPer()) > 0)) return false;
        if (f.getMinPbr() != null && (indicator.getPbr() == null || indicator.getPbr().compareTo(f.getMinPbr()) < 0)) return false;
        if (f.getMaxPbr() != null && (indicator.getPbr() == null || indicator.getPbr().compareTo(f.getMaxPbr()) > 0)) return false;
        if (f.getMinRoe() != null && (indicator.getRoe() == null || indicator.getRoe().compareTo(f.getMinRoe()) < 0)) return false;
        
        return true;
    }
}
