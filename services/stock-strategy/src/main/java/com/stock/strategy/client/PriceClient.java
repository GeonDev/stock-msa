package com.stock.strategy.client;

import com.stock.common.dto.StockIndicatorDto;
import com.stock.common.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceClient {

    private final RestClient restClient;

    @Value("${services.stock-price.url:http://localhost:8083}")
    private String priceServiceUrl;

    private static final int BATCH_SIZE = 200;

    public List<StockPriceDto> getPriceHistory(String stockCode, String startDate, String endDate) {
        return restClient.get()
                .uri(priceServiceUrl + "/internal/prices/" + stockCode
                        + "?startDate=" + startDate + "&endDate=" + endDate)
                .retrieve()
                .body(new ParameterizedTypeReference<List<StockPriceDto>>() {});
    }

    @Cacheable(value = "priceCache", key = "'stockCode:' + #stockCode + ':date:' + #date")
    public StockPriceDto getPriceByDate(String stockCode, String date) {
        return restClient.get()
                .uri(priceServiceUrl + "/internal/price/" + stockCode + "/" + date)
                .retrieve()
                .body(StockPriceDto.class);
    }

    public List<StockPriceDto> getPricesByDateBatch(List<String> stockCodes, String date) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }

        List<StockPriceDto> allResults = new ArrayList<>();
        for (int i = 0; i < stockCodes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, stockCodes.size());
            List<String> chunk = stockCodes.subList(i, end);
            String stockCodesParam = String.join(",", chunk);
            
            try {
                List<StockPriceDto> chunkResult = restClient.get()
                        .uri(priceServiceUrl + "/internal/prices/batch?stockCodes=" + stockCodesParam + "&date=" + date)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<StockPriceDto>>() {});
                
                if (chunkResult != null) {
                    allResults.addAll(chunkResult);
                }
            } catch (Exception e) {
                log.error("Failed to fetch price batch chunk for date {}: {}", date, e.getMessage());
            }
        }
        return allResults;
    }

    public List<StockIndicatorDto> getIndicatorsByDateBatch(List<String> stockCodes, String date) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }

        List<StockIndicatorDto> allResults = new ArrayList<>();
        for (int i = 0; i < stockCodes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, stockCodes.size());
            List<String> chunk = stockCodes.subList(i, end);
            String stockCodesParam = String.join(",", chunk);
            
            try {
                String uri = priceServiceUrl + "/internal/indicators/batch?stockCodes=" + stockCodesParam + "&date=" + date;
                log.info("Calling Price Service: {}", uri);
                
                List<StockIndicatorDto> chunkResult = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<StockIndicatorDto>>() {});
                
                if (chunkResult != null) {
                    allResults.addAll(chunkResult);
                }
            } catch (Exception e) {
                log.error("Failed to fetch indicator batch chunk for date {}: {}", date, e.getMessage());
            }
        }
        return allResults;
    }
}
