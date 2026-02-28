package com.stock.price.service;

import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCapRankService {

    private final StockPriceRepository stockPriceRepository;

    @Transactional
    public void calculateAndSaveMarketCapRanks(LocalDate date) {
        log.info("Calculating market cap ranks for date: {}", date);
        List<StockPrice> prices = stockPriceRepository.findByBasDt(date);
        
        if (prices.isEmpty()) {
            log.warn("No price data found for date: {}", date);
            return;
        }

        // 시장별로 그룹화 (KOSPI, KOSDAQ 등)
        Map<String, List<StockPrice>> pricesByMarket = prices.stream()
                .filter(p -> p.getMarketTotalAmt() != null)
                .collect(Collectors.groupingBy(p -> p.getMarketCode() != null ? p.getMarketCode() : "UNKNOWN"));

        for (Map.Entry<String, List<StockPrice>> entry : pricesByMarket.entrySet()) {
            List<StockPrice> marketPrices = entry.getValue();

            // 시가총액 오름차순 정렬 (작은 것부터 큰 것 순)
            // percentile 0: 가장 작은 종목, 100: 가장 큰 종목
            marketPrices.sort(Comparator.comparing(StockPrice::getMarketTotalAmt));

            int totalCount = marketPrices.size();
            for (int i = 0; i < totalCount; i++) {
                StockPrice price = marketPrices.get(i);
                
                // 순위 (내림차순 순위: 큰 것이 1등)
                price.setMarketCapRank(totalCount - i);
                
                // 분위수 (0.0 ~ 100.0)
                // i=0 (가장 작은 종목) -> 0.0
                // i=totalCount-1 (가장 큰 종목) -> 100.0
                BigDecimal percentile = BigDecimal.valueOf(i)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(Math.max(1, totalCount - 1)), 2, RoundingMode.HALF_UP);
                
                price.setMarketCapPercentile(percentile);
            }
        }

        stockPriceRepository.saveAll(prices);
        log.info("Successfully saved market cap ranks for date: {}", date);
    }
}
