package com.stock.batch.batchJob.ItemReader;

import com.stock.batch.entity.StockPrice;
import com.stock.batch.enums.StockType;
import com.stock.batch.service.StockApiService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class StockApiItemReader implements ItemReader<StockPrice> {

    private final StockApiService stockApiService;
    private Iterator<StockPrice> stockIterator;
    private boolean dataFetched = false;

    // @Value를 사용하여 JobParameter 받기
    @Value("#{jobParameters['date']}")
    private String jobDate;

    @Value("#{jobParameters['market']}")
    private String jobMarket;

    @Override
    public StockPrice read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!dataFetched) {
            List<StockPrice> list = stockApiService.getStockPrice(StockType.valueOf(jobMarket), jobDate);
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
