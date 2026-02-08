package com.stock.price.controller;

import com.stock.common.dto.StockPriceDto;
import com.stock.price.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stock/internal")
public class StockInternalController {

    private final StockService stockService;

    @GetMapping("/price/latest/{stockCode}")
    public StockPriceDto getLatestStockPrice(@PathVariable String stockCode) {
        return stockService.getLatestStockPrice(stockCode);
    }

    @GetMapping("/price/{stockCode}/{date}")
    public StockPriceDto getPriceByDate(@PathVariable String stockCode, @PathVariable String date) {
        return stockService.getPriceByDate(stockCode, date);
    }
}
