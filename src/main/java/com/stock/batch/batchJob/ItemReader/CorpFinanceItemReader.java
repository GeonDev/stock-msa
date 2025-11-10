package com.stock.batch.batchJob.ItemReader;

import com.stock.batch.entity.CorpFinance;
import com.stock.batch.service.StockApiService;
import com.stock.batch.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class CorpFinanceItemReader implements ItemReader<CorpFinance> {

    private final StockApiService stockApiService;
    private Iterator<CorpFinance> corpIterator;
    private boolean dataFetched = false;

    // @Value를 사용하여 JobParameter 받기
    @Value("#{jobParameters['date']}")
    private String jobDate;

    @Override
    public CorpFinance read() throws Exception {
        if (!dataFetched) {
            //연도값만 추출
            LocalDate date = DateUtils.toStringLocalDate(jobDate);
            List<CorpFinance> list = stockApiService.getCorpFinance(String.valueOf(date.getYear()));
            if (list != null && !list.isEmpty()) {
                corpIterator = list.iterator();
            }
            dataFetched = true;
        }

        if (corpIterator != null && corpIterator.hasNext()) {
            return corpIterator.next();
        }

        return null;
    }
}
