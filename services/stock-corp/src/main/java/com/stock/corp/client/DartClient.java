package com.stock.corp.client;

import com.stock.common.consts.ApplicationConstants;
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

    @Value("${dart.api-key}")
    private String apiKey;

    public DartCompanyResponse getCompanyInfo(String corpCode) {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("http")
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
