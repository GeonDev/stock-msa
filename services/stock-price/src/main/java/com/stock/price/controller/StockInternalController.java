package com.stock.price.controller;

import com.stock.common.dto.StockPriceDto;
import com.stock.price.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
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

    @GetMapping("/prices/{stockCode}")
    public java.util.List<StockPriceDto> getPriceHistory(
            @PathVariable String stockCode, 
            @RequestParam(name = "days", defaultValue = "365") int days,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        if (startDate != null && endDate != null) {
            return stockService.getPriceHistory(stockCode, startDate, endDate);
        }
        return stockService.getPriceHistory(stockCode, days);
    }

    @GetMapping("/prices/batch")
    public java.util.List<StockPriceDto> getPricesByDateBatch(@RequestParam java.util.List<String> stockCodes, @RequestParam String date) {
        return stockService.getPricesByDateBatch(stockCodes, date);
    }

    @GetMapping("/indicators/batch")
    public java.util.List<com.stock.common.dto.StockIndicatorDto> getIndicatorsByDateBatch(@RequestParam java.util.List<String> stockCodes, @RequestParam String date) {
        return stockService.getIndicatorsByDateBatch(stockCodes, date);
    }
}
