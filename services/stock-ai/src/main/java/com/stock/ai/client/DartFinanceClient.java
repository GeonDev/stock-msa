package com.stock.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class DartFinanceClient {

    private final WebClient webClient;

    public DartFinanceClient(WebClient.Builder webClientBuilder, @Value("${services.stock-finance.url}") String financeUrl) {
        this.webClient = webClientBuilder.baseUrl(financeUrl).build();
    }

    public Mono<DartListResponse> getDisclosureList(String bgnDe, String endDe, String pblntfTy) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/dart/list")
                        .queryParam("bgnDe", bgnDe)
                        .queryParam("endDe", endDe)
                        .queryParam("pblntfTy", pblntfTy)
                        .build())
                .retrieve()
                .bodyToMono(DartListResponse.class);
    }

    public Mono<String> triggerFinanceBatch(String corpCode, String reportCode) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/batch/corp-fin")
                        .queryParam("corpCode", corpCode)
                        .queryParam("reportCode", reportCode)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("Failed to trigger finance batch for corpCode: {}", corpCode, e);
                    return Mono.just("FAILED");
                });
    }

    public record DartListResponse(
        String status,
        String message,
        List<DartDisclosure> list
    ) {}

    public record DartDisclosure(
        String corp_code,
        String corp_name,
        String stock_code,
        String report_nm,
        String rcept_no,
        String rcept_dt
    ) {}
}
