package com.stock.finance.client;

import com.stock.common.dto.CorpInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorpClient {

    private final RestClient restClient;

    @Value("${services.stock-corp.url:http://localhost:8081}")
    private String corpServiceUrl;

    public CorpInfoDto getCorpInfo(String corpCode) {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/api/v1/corp/internal/" + corpCode)
                    .retrieve()
                    .body(CorpInfoDto.class);
        } catch (Exception e) {
            log.error("Failed to get corp info for: {}", corpCode, e);
            return null;
        }
    }

    public Set<String> getValidCorpCodes() {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/api/v1/corp/internal/valid-codes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Set<String>>() {});
        } catch (Exception e) {
            log.error("Failed to get valid corp codes", e);
            return Set.of();
        }
    }
    
    /**
     * 전체 종목코드 조회 (DART API용)
     */
    public List<String> getAllStockCodes() {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/api/v1/corp/internal/all-stock-codes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to get all stock codes", e);
            return List.of();
        }
    }

    /**
     * DART 고유번호 조회 (stock-corp 서비스 API 호출 → DB 조회)
     */
    public String getDartCorpCode(String stockCode) {
        try {
            return restClient.get()
                    .uri(corpServiceUrl + "/api/v1/corp/internal/dart-corp-code/" + stockCode)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to get DART corp code for: {}", stockCode, e);
            return null;
        }
    }
}
