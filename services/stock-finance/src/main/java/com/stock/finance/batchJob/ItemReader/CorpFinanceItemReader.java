package com.stock.finance.batchJob.ItemReader;

import com.stock.common.enums.ReportCode;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.client.CorpClient;
import com.stock.finance.service.CorpFinanceService;
import com.stock.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class CorpFinanceItemReader implements ItemReader<CorpFinance> {

    private final CorpFinanceService corpFinanceService;
    private final CorpClient corpClient;
    private Iterator<CorpFinance> corpIterator;
    private boolean dataFetched = false;

    @Value("#{jobParameters['date']}")
    private String jobDate;

    @Value("#{jobParameters['reportCode']}")
    private String reportCodeParam;

    @Override
    public CorpFinance read() throws Exception {
        if (!dataFetched) {
            LocalDate date = DateUtils.toStringLocalDate(jobDate);
            String bizYear = String.valueOf(date.getYear());

            ReportCode reportCode = (reportCodeParam != null && !reportCodeParam.isBlank())
                    ? ReportCode.valueOf(reportCodeParam)
                    : null;

            List<CorpFinance> list = corpFinanceService.getCorpFinance(bizYear, reportCode);
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
