package com.stock.strategy.client;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestClient restClient;

    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;

    private static final int BATCH_SIZE = 200;

    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(List<String> corpCodes, String date) {
        if (corpCodes == null || corpCodes.isEmpty()) {
            return List.of();
        }

        List<CorpFinanceIndicatorDto> allResults = new ArrayList<>();
        
        for (int i = 0; i < corpCodes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, corpCodes.size());
            List<String> chunk = corpCodes.subList(i, end);
            String corpCodesParam = String.join(",", chunk);
            
            try {
                List<CorpFinanceIndicatorDto> chunkResult = restClient.get()
                        .uri(financeServiceUrl + "/internal/indicators/batch?corpCodes=" 
                                + corpCodesParam + "&date=" + date)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CorpFinanceIndicatorDto>>() {});
                
                if (chunkResult != null) {
                    allResults.addAll(chunkResult);
                }
            } catch (Exception e) {
                log.error("Failed to fetch indicators batch chunk for date {}: {}", date, e.getMessage());
            }
        }
        
        return allResults;
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
