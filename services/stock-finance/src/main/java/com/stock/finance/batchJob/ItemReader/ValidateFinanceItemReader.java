package com.stock.finance.batchJob.ItemReader;

import com.stock.finance.entity.CorpFinance;
import com.stock.finance.repository.CorpFinanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ValidateFinanceItemReader implements ItemReader<CorpFinance> {

    private final CorpFinanceRepository corpFinanceRepository;
    private Iterator<CorpFinance> financeIterator;
    private boolean dataFetched = false;

    @Override
    public CorpFinance read() {
        if (!dataFetched) {
            log.info("Fetching companies for validation...");
            List<CorpFinance> list = corpFinanceRepository.findByValidationStatusIsNull();
            
            if (list != null && !list.isEmpty()) {
                log.info("Found {} companies to validate", list.size());
                financeIterator = list.iterator();
            } else {
                log.warn("No companies found with NULL validation status");
            }
            dataFetched = true;
        }

        if (financeIterator != null && financeIterator.hasNext()) {
            return financeIterator.next();
        }

        return null;
    }
}
