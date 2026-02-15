package com.stock.strategy.client;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestClient restClient;

    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;

    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(List<String> corpCodes, String date) {
        String corpCodesParam = String.join(",", corpCodes);
        
        try {
            return restClient.get()
                    .uri(financeServiceUrl + "/internal/indicators/batch?corpCodes=" 
                            + corpCodesParam + "&date=" + date)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CorpFinanceIndicatorDto>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch indicators batch for date {}: {}", date, e.getMessage());
            return List.of();
        }
    }

    public CorpFinanceIndicatorDto getLatestIndicator(String corpCode) {
        try {
            return restClient.get()
                    .uri(financeServiceUrl + "/internal/indicators/" + corpCode + "/latest")
                    .retrieve()
                    .body(CorpFinanceIndicatorDto.class);
        } catch (Exception e) {
            log.warn("Failed to fetch latest indicator for {}: {}", corpCode, e.getMessage());
            return null;
        }
    }
}
