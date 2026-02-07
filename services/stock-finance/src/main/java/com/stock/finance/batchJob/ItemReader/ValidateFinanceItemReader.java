package com.stock.finance.batchJob.ItemReader;

import com.stock.finance.entity.CorpFinance;
import com.stock.finance.repository.CorpFinanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class ValidateFinanceItemReader implements ItemReader<CorpFinance> {

    private final CorpFinanceRepository corpFinanceRepository;
    private Iterator<CorpFinance> financeIterator;
    private boolean dataFetched = false;

    @Override
    public CorpFinance read() {
        if (!dataFetched) {
            List<CorpFinance> list = corpFinanceRepository.findByValidationStatusIsNull();
            
            if (list != null && !list.isEmpty()) {
                financeIterator = list.iterator();
            }
            dataFetched = true;
        }

        if (financeIterator != null && financeIterator.hasNext()) {
            return financeIterator.next();
        }

        return null;
    }
}
