package com.stock.strategy.client;

import com.stock.common.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceClient {

    private final RestClient restClient;

    @Value("${services.stock-price.url:http://localhost:8083}")
    private String priceServiceUrl;

    public List<StockPriceDto> getPriceHistory(String stockCode, String startDate, String endDate) {
        return restClient.get()
                .uri(priceServiceUrl + "/api/v1/stock/internal/prices/" + stockCode
                        + "?startDate=" + startDate + "&endDate=" + endDate)
                .retrieve()
                .body(new ParameterizedTypeReference<List<StockPriceDto>>() {});
    }

    public StockPriceDto getPriceByDate(String stockCode, String date) {
        return restClient.get()
                .uri(priceServiceUrl + "/api/v1/stock/internal/price/" + stockCode + "/" + date)
                .retrieve()
                .body(StockPriceDto.class);
    }
}
