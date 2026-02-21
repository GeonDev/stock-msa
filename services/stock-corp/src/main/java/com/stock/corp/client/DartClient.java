package com.stock.corp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.consts.ApplicationConstants;
import com.stock.common.util.DartRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DartRateLimiter rateLimiter = new DartRateLimiter();

    @Value("${dart.api-key}")
    private String apiKey;

    public DartCompanyResponse getCompanyInfo(String corpCode) {
        // Rate limit 체크 (분당 1,000회)
        rateLimiter.acquire();
        
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(ApplicationConstants.DART_URL)
                .path(ApplicationConstants.DART_CORP_INFO_URL)
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .build()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DartCompanyResponse.class);
        } catch (Exception e) {
            log.error("DART API 호출 실패 - corpCode: {}, error: {}", corpCode, e.getMessage());
            return null;
        }
    }
}
