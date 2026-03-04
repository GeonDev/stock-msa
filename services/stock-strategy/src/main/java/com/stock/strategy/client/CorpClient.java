package com.stock.strategy.client;

import com.stock.common.dto.CorpInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorpClient {

    private final RestClient restClient;

    @Value("${services.stock-corp.url:http://localhost:8081}")
    private String corpServiceUrl;

    private static final int BATCH_SIZE = 200;

    @Cacheable(value = "corpCache", key = "'market:' + #market + ':date:' + #date")
    public List<CorpInfoDto> getCorpsByMarket(String market, String date) {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/internal/corps?market=" + market + "&date=" + date)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CorpInfoDto>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch corps by market {}: {}", market, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "corpCache", key = "'corpCode:' + #corpCode")
    public CorpInfoDto getCorpInfo(String corpCode) {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/internal/" + corpCode)
                    .retrieve()
                    .body(CorpInfoDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch corp info for {}: {}", corpCode, e.getMessage());
            return null;
        }
    }

    public List<CorpInfoDto> getCorpsBatch(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }

        List<CorpInfoDto> allResults = new ArrayList<>();
        for (int i = 0; i < stockCodes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, stockCodes.size());
            List<String> chunk = stockCodes.subList(i, end);
            String stockCodesParam = String.join(",", chunk);
            
            try {
                List<CorpInfoDto> chunkResult = restClient.get()
                        .uri(corpServiceUrl + "/internal/batch?stockCodes=" + stockCodesParam)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CorpInfoDto>>() {});
                
                if (chunkResult != null) {
                    allResults.addAll(chunkResult);
                }
            } catch (Exception e) {
                log.error("Failed to fetch corps batch chunk: {}", e.getMessage());
            }
        }
        return allResults;
    }
}
