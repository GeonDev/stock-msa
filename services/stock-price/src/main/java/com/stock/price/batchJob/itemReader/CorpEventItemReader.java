package com.stock.price.batchJob.itemReader;

import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class CorpEventItemReader implements ItemReader<String> {

    private final StockPriceRepository stockPriceRepository;
    private Iterator<String> stockCodeIterator;
    private boolean dataFetched = false;

    @Value("#{jobParameters['targetDate']}")
    private String targetDate;

    @Override
    public String read() throws Exception {
        if (!dataFetched) {
            LocalDate date = (targetDate != null) ? LocalDate.parse(targetDate) : LocalDate.now();
            List<String> stockCodes = stockPriceRepository.findDistinctStockCodeByBasDtBetween(date, date);
            
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
