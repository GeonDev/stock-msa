package com.stock.price.controller;

import com.stock.common.dto.StockPriceDto;
import com.stock.price.entity.StockPrice;
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
        StockPrice entity = stockService.getLatestStockPrice(stockCode);
        if (entity == null) return null;

        return StockPriceDto.builder()
                .id(entity.getId())
                .stockCode(entity.getStockCode())
                .marketCode(entity.getMarketCode())
                .basDt(entity.getBasDt())
                .volume(entity.getVolume())
                .volumePrice(entity.getVolumePrice())
                .startPrice(entity.getStartPrice())
                .endPrice(entity.getEndPrice())
                .highPrice(entity.getHighPrice())
                .lowPrice(entity.getLowPrice())
                .dailyRange(entity.getDailyRange())
                .dailyRatio(entity.getDailyRatio())
                .stockTotalCnt(entity.getStockTotalCnt())
                .marketTotalAmt(entity.getMarketTotalAmt())
                .build();
    }
}
