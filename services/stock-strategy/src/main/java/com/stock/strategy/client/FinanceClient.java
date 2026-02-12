package com.stock.strategy.client;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestClient restClient;

    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;

    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(List<String> corpCodes, String date) {
        String corpCodesParam = String.join(",", corpCodes);
        
        return restClient.get()
                .uri(financeServiceUrl + "/api/v1/finance/internal/indicators/batch?corpCodes=" 
                        + corpCodesParam + "&date=" + date)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CorpFinanceIndicatorDto>>() {});
    }
}
