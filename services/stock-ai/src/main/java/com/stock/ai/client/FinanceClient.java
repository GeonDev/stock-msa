package com.stock.ai.client;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FinanceClient {

    private final WebClient webClient;

    public FinanceClient(WebClient.Builder webClientBuilder, @Value("${services.stock-finance.url}") String financeUrl) {
        this.webClient = webClientBuilder.baseUrl(financeUrl).build();
    }

    public Mono<CorpFinanceIndicatorDto> getLatestIndicator(String corpCode) {
        return webClient.get()
                .uri("/internal/indicators/{corpCode}/latest", corpCode)
                .retrieve()
                .bodyToMono(CorpFinanceIndicatorDto.class)
                .onErrorResume(e -> Mono.empty());
    }
}
