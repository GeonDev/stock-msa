package com.stock.finance.client;

import com.stock.common.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class StockClient {

    private final RestClient restClient;

    @Value("${services.stock-price.url:http://localhost:8083}")
    private String stockServiceUrl;

    public StockPriceDto getLatestStockPrice(String stockCode) {
        return restClient.get()
                .uri(stockServiceUrl + "/api/v1/stock/internal/price/latest/" + stockCode)
                .retrieve()
                .body(StockPriceDto.class);
    }
}
