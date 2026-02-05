package com.stock.price.batchJob.itemReader;

import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class WeeklyStockCodeItemReader implements ItemReader<String> {

    private final StockPriceRepository stockPriceRepository;
    private Iterator<String> stockCodeIterator;
    private boolean dataFetched = false;

    @Override
    public String read() throws Exception {
        if (!dataFetched) {
            List<String> stockCodes = stockPriceRepository.findDistinctStockCodes();
            
            if (stockCodes != null && !stockCodes.isEmpty()) {
                stockCodeIterator = stockCodes.iterator();
            }
            dataFetched = true;
        }

        if (stockCodeIterator != null && stockCodeIterator.hasNext()) {
            return stockCodeIterator.next();
        }

        return null;
    }
}
