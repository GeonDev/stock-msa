package com.stock.price.batchJob.itemReader;

import com.stock.price.entity.StockPrice;
import com.stock.common.enums.StockMarket;
import com.stock.price.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;

import com.stock.common.utils.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class StockPriceItemReader implements ItemReader<StockPrice> {

    private final StockService stockService;
    private Iterator<StockPrice> stockIterator;
    private boolean dataFetched = false;

    // @Value를 사용하여 JobParameter 받기
    @Value("#{jobParameters['date']}")
    private String jobDate;

    @Value("#{jobParameters['market']}")
    private String jobMarket;

    @Override
    public synchronized StockPrice read() throws Exception {
        if (!dataFetched) {
            List<StockPrice> allPrices = new ArrayList<>();
            LocalDate endDate = DateUtils.toStringLocalDate(jobDate);
            // Collect for last 7 days
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = endDate.minusDays(i);
                String targetBasDt = DateUtils.toLocalDateString(targetDate);
                try {
                    List<StockPrice> dailyPrices = stockService.getStockPrice(StockMarket.valueOf(jobMarket), targetBasDt);
                    if (dailyPrices != null && !dailyPrices.isEmpty()) {
                        allPrices.addAll(dailyPrices);
                        log.info("Fetched {} prices for date {}", dailyPrices.size(), targetBasDt);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch prices for date {}: {}", targetBasDt, e.getMessage());
                }
            }
            
            if (!allPrices.isEmpty()) {
                stockIterator = allPrices.iterator();
            }
            dataFetched = true;
        }

        if (stockIterator != null && stockIterator.hasNext()) {
            return stockIterator.next();
        }

        return null;
    }
}
