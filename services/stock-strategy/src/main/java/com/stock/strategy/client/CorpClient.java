package com.stock.strategy.client;

import com.stock.common.dto.CorpInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CorpClient {

    private final RestClient restClient;

    @Value("${services.stock-corp.url:http://localhost:8081}")
    private String corpServiceUrl;

    public List<CorpInfoDto> getCorpsByMarket(String market, String date) {
        return restClient.get()
                .uri(corpServiceUrl + "/api/v1/corp/internal/corps?market=" + market + "&date=" + date)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CorpInfoDto>>() {});
    }

    public CorpInfoDto getCorpInfo(String corpCode) {
        return restClient.get()
                .uri(corpServiceUrl + "/api/v1/corp/internal/" + corpCode)
                .retrieve()
                .body(CorpInfoDto.class);
    }
}
