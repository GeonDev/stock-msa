package com.stock.finance.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.consts.ApplicationConstants;
import com.stock.common.util.DartRateLimiter;
import com.stock.finance.dto.DartFinancialResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class DartClient {
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DartRateLimiter rateLimiter = new DartRateLimiter();
    
    @Value("${dart.api-key}")
    private String apiKey;

    public DartClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 단일회사 전체 재무제표 조회
     * 
     * @param corpCode 고유번호 (8자리)
     * @param year 사업연도 (YYYY)
     * @param reportCode 보고서코드 (11011:사업보고서, 11012:반기, 11013:1분기, 11014:3분기)
     * @param fsDiv 개별/연결구분 (CFS:연결, OFS:개별)
     * @return 재무제표 응답
     */
    public DartFinancialResponse getFinancialStatement(
            String corpCode,
            String year,
            String reportCode,
            String fsDiv) {
        
        // Rate limit 체크 (분당 1,000회)
        rateLimiter.acquire();
        
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(ApplicationConstants.DART_URL)
                .path(ApplicationConstants.DART_FINANCE_URL)
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", year)
                .queryParam("reprt_code", reportCode)
                .queryParam("fs_div", fsDiv)
                .build()
                .toUri();
        
        try {
            DartFinancialResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DartFinancialResponse.class);
            
            // 응답 상태 확인
            if (response != null && !"000".equals(response.getStatus())) {
                log.warn("DART API returned non-success status: {} - {}", response.getStatus(), response.getMessage());
                return null;
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch DART financial statement for corp: {}, year: {}", corpCode, year, e);
            return null;
        }
    }
}
