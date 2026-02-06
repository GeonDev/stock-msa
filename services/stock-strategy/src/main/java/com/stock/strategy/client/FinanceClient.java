package com.stock.strategy.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestClient restClient;

    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;

    // 필요시 재무 데이터 조회 메서드 추가
}
