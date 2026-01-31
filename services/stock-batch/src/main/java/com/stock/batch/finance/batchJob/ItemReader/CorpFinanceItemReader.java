package com.stock.batch.finance.batchJob.ItemReader;

import com.stock.batch.finance.entity.CorpFinance;
import com.stock.batch.corp.entity.CorpInfo;
import com.stock.batch.corp.repository.CorpInfoRepository;
import com.stock.batch.finance.service.CorpFinanceService;
import com.stock.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CorpFinanceItemReader implements ItemReader<CorpFinance> {

    private final CorpFinanceService corpFinanceService;
    private final CorpInfoRepository corpInfoRepository;
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
                Set<String> validCorpCodes = corpInfoRepository.findAllByStockCodeIsNotNull()
                        .stream()
                        .map(CorpInfo::getCorpCode)
                        .collect(Collectors.toSet());

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
