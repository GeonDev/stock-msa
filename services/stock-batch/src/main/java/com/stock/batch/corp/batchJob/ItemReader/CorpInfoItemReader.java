package com.stock.batch.corp.batchJob.ItemReader;

import com.stock.batch.corp.entity.CorpInfo;
import com.stock.batch.corp.service.CorpInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class CorpInfoItemReader implements ItemReader<CorpInfo> {

    private final CorpInfoService corpInfoService;
    private Iterator<CorpInfo> corpIterator;
    private boolean dataFetched = false;

    // @Value를 사용하여 JobParameter 받기
    @Value("#{jobParameters['date']}")
    private String jobDate;

    @Override
    public CorpInfo read() throws Exception {
        if (!dataFetched) {
            List<CorpInfo> list = corpInfoService.getCorpInfo(jobDate);
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
