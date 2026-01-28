package com.stock.batch.stock.batchJob.itemReader;

import com.stock.batch.stock.entity.StockPrice;
import com.stock.batch.global.enums.StockMarket;
import com.stock.batch.stock.service.StockService;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;

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
    public StockPrice read() throws Exception {
        if (!dataFetched) {
            List<StockPrice> list = stockService.getStockPrice(StockMarket.valueOf(jobMarket), jobDate);
            if (list != null && !list.isEmpty()) {
                stockIterator = list.iterator();
            }
            dataFetched = true;
        }

        if (stockIterator != null && stockIterator.hasNext()) {
            return stockIterator.next();
        }

        return null;
    }
}
