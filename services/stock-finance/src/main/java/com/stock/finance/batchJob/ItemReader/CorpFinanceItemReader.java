package com.stock.finance.batchJob.ItemReader;

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
import java.util.Set;

@RequiredArgsConstructor
public class CorpFinanceItemReader implements ItemReader<CorpFinance> {

    private final CorpFinanceService corpFinanceService;
    private final CorpClient corpClient;
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
            List<CorpFinance> list = corpFinanceService.getCorpFinance(String.valueOf(date.getYear()));

            if (list != null && !list.isEmpty()) {
                // 상장된 회사(stockCode가 있는 회사)의 corpCode 목록 조회
                Set<String> validCorpCodes = corpClient.getValidCorpCodes();

                // 상장된 회사 정보만 필터링
                List<CorpFinance> filteredList = list.stream()
                        .filter(f -> validCorpCodes.contains(f.getCorpCode()))
                        .toList();

                corpIterator = filteredList.iterator();
            }
            dataFetched = true;
        }

        if (corpIterator != null && corpIterator.hasNext()) {
            return corpIterator.next();
        }

        return null;
    }
}
