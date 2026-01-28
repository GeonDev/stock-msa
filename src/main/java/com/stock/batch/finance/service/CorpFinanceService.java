package com.stock.batch.finance.service;

import com.stock.batch.finance.entity.CorpFinanceIndicator;
import com.stock.batch.finance.repository.CorpFinanceRepository;
import com.stock.batch.global.consts.ApplicationConstants;
import com.stock.batch.finance.entity.CorpFinance;
import com.stock.batch.global.utils.ParseUtils;
import com.stock.batch.global.model.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorpFinanceService {

    @Value("${data-go.service-key}")
    String serviceKey;

    private final RestClient restClient;
    private final CorpFinanceRepository corpFinanceRepository;

    public List<CorpFinance> getCorpFinance(String bizYear) throws Exception {
        List<CorpFinance> corpList = new ArrayList<>();
        int pageNum = 1;
        int totalPage = 1;

        String decodedServiceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);

        while (totalPage >= pageNum) {
            UriComponents uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("https")
                    .host(ApplicationConstants.API_GO_URL)
                    .path(ApplicationConstants.KRX_STOCK_FINANCE_URI)
                    .queryParam("serviceKey", decodedServiceKey)
                    .queryParam("numOfRows", ApplicationConstants.PAGE_SIZE)
                    .queryParam("pageNo", pageNum)
                    .queryParam("bizYear", bizYear)
                    .build();

            String responseBody = restClient.get()
                    .uri(uri.toUri())
                    .retrieve()
                    .body(String.class);

            try {
                ApiBody<CorpFinance> result = ParseUtils.parseCorpFinanceFromXml(responseBody);
                log.debug("pageNum : {} totalPage : {}" , pageNum, totalPage);
                if(pageNum == 1){
                    totalPage = (int) Math.ceil((double) result.getTotalCount() / ApplicationConstants.PAGE_SIZE);
                }
                corpList.addAll(result.getItemList());
                pageNum++;
            } catch (Exception e) {
                log.error("Failed to parse XML response: {}", responseBody);
                throw e;
            }
        }

        return corpList;
    }

    public CorpFinanceIndicator calculateIndicators(CorpFinance currentFinance, Long marketCap) {
        CorpFinanceIndicator.CorpFinanceIndicatorBuilder builder = CorpFinanceIndicator.builder()
                .corpCode(currentFinance.getCorpCode())
                .basDt(currentFinance.getBasDt());

        // Calculate ROE, ROA, Debt Ratio from current data
        if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital() != 0) {
            if (currentFinance.getNetIncome() != null) {
                builder.roe((double) currentFinance.getNetIncome() / currentFinance.getTotalCapital() * 100);
            }
        }
        if (currentFinance.getTotalAsset() != null && currentFinance.getTotalAsset() != 0 && currentFinance.getNetIncome() != null) {
            builder.roa((double) currentFinance.getNetIncome() / currentFinance.getTotalAsset() * 100);
        }

        // Calculate PER, PBR, PSR from current data and market cap
        if (marketCap != null && marketCap > 0) {
            if (currentFinance.getNetIncome() != null && currentFinance.getNetIncome() > 0) {
                builder.per((double) marketCap / currentFinance.getNetIncome());
            }
            if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital() > 0) {
                builder.pbr((double) marketCap / currentFinance.getTotalCapital());
            }
            if (currentFinance.getRevenue() != null && currentFinance.getRevenue() > 0) {
                builder.psr((double) marketCap / currentFinance.getRevenue());
            }
        }

        // Fetch previous finance data to calculate growth rates
        Optional<CorpFinance> prevFinanceOpt = corpFinanceRepository.findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(currentFinance.getCorpCode(), currentFinance.getBasDt());
        if (prevFinanceOpt.isPresent()) {
            CorpFinance prevFinance = prevFinanceOpt.get();
            builder.revenueGrowth(calculateGrowthRate(currentFinance.getRevenue(), prevFinance.getRevenue()));
            builder.netIncomeGrowth(calculateGrowthRate(currentFinance.getNetIncome(), prevFinance.getNetIncome()));
            builder.opIncomeGrowth(calculateGrowthRate(currentFinance.getOpIncome(), prevFinance.getOpIncome()));
        }

        return builder.build();
    }

    private Double calculateGrowthRate(Long currentValue, Long previousValue) {
        if (currentValue == null || previousValue == null || previousValue == 0) {
            return null;
        }
        return ((double) currentValue / previousValue - 1) * 100;
    }

}