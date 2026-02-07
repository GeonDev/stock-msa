package com.stock.corp.batchJob.ItemReader;

import com.stock.corp.entity.CorpInfo;
import com.stock.corp.repository.CorpInfoRepository;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;

import java.util.Collections;

public class CorpDetailItemReader extends RepositoryItemReader<CorpInfo> {

    public CorpDetailItemReader(CorpInfoRepository corpInfoRepository) {
        setRepository(corpInfoRepository);
        setMethodName("findAll");
        setPageSize(100);
        setSort(Collections.singletonMap("corpCode", Sort.Direction.ASC));
    }
}
