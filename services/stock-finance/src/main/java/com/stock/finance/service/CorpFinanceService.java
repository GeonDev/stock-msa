package com.stock.finance.service;

import com.stock.finance.entity.CorpFinanceIndicator;
import com.stock.finance.repository.CorpFinanceRepository;
import com.stock.common.consts.ApplicationConstants;
import com.stock.finance.entity.CorpFinance;
import com.stock.common.model.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.stock.finance.utils.FinanceParseUtils.parseCorpFinanceFromXml;

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
                ApiBody<CorpFinance> result = parseCorpFinanceFromXml(responseBody);
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

    public CorpFinanceIndicator calculateIndicators(CorpFinance currentFinance, BigDecimal marketCap) {
        CorpFinanceIndicator.CorpFinanceIndicatorBuilder builder = CorpFinanceIndicator.builder()
                .corpCode(currentFinance.getCorpCode())
                .basDt(currentFinance.getBasDt());

        // Calculate ROE, ROA, Debt Ratio from current data
        if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital().compareTo(BigDecimal.ZERO) != 0) {
            if (currentFinance.getNetIncome() != null) {
                builder.roe(currentFinance.getNetIncome()
                        .divide(currentFinance.getTotalCapital(), 8, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
        }
        if (currentFinance.getTotalAsset() != null && currentFinance.getTotalAsset().compareTo(BigDecimal.ZERO) != 0 && currentFinance.getNetIncome() != null) {
            builder.roa(currentFinance.getNetIncome()
                    .divide(currentFinance.getTotalAsset(), 8, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        // Calculate PER, PBR, PSR from current data and market cap
        if (marketCap != null && marketCap.compareTo(BigDecimal.ZERO) > 0) {
            if (currentFinance.getNetIncome() != null && currentFinance.getNetIncome().compareTo(BigDecimal.ZERO) > 0) {
                builder.per(marketCap.divide(currentFinance.getNetIncome(), 8, java.math.RoundingMode.HALF_UP));
            }
            if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital().compareTo(BigDecimal.ZERO) > 0) {
                builder.pbr(marketCap.divide(currentFinance.getTotalCapital(), 8, java.math.RoundingMode.HALF_UP));
            }
            if (currentFinance.getRevenue() != null && currentFinance.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                builder.psr(marketCap.divide(currentFinance.getRevenue(), 8, java.math.RoundingMode.HALF_UP));
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

    private BigDecimal calculateGrowthRate(BigDecimal currentValue, BigDecimal previousValue) {
        if (currentValue == null || previousValue == null || previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentValue.divide(previousValue, 8, java.math.RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100));
    }

}