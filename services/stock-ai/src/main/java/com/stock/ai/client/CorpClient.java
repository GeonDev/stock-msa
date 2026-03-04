package com.stock.ai.client;

import com.stock.common.dto.CorpInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CorpClient {

    private final WebClient webClient;

    public CorpClient(WebClient.Builder webClientBuilder, @Value("${services.stock-corp.url}") String corpUrl) {
        this.webClient = webClientBuilder.baseUrl(corpUrl).build();
    }

    public Mono<CorpInfoDto> getCorpInfo(String stockCode) {
        return webClient.get()
                .uri("/internal/corp-detail/{stockCode}", stockCode)
                .retrieve()
                .bodyToMono(CorpInfoDto.class)
                .onErrorResume(e -> Mono.empty());
    }
}
