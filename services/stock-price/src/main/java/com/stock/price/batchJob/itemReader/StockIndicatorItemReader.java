package com.stock.price.batchJob.itemReader;

import com.stock.price.entity.StockPrice;
import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class StockIndicatorItemReader implements ItemReader<StockPrice> {

    private final StockPriceRepository stockPriceRepository;
    private Iterator<StockPrice> stockPriceIterator;
    private boolean dataFetched = false;

    @Value("#{jobParameters['targetDate']}")
    private String targetDate;

    @Override
    public StockPrice read() throws Exception {
        if (!dataFetched) {
            LocalDate date = (targetDate != null) ? LocalDate.parse(targetDate) : LocalDate.now();
            List<StockPrice> stockPrices = stockPriceRepository.findByBasDt(date);
            
            if (stockPrices != null && !stockPrices.isEmpty()) {
                stockPriceIterator = stockPrices.iterator();
            }
            dataFetched = true;
        }

        if (stockPriceIterator != null && stockPriceIterator.hasNext()) {
            return stockPriceIterator.next();
        }

        return null;
    }
}
