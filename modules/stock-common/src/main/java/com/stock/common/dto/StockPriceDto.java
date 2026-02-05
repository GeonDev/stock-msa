package com.stock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockPriceDto {
    private Long id;
    private String stockCode;
    private String marketCode;
    private LocalDate basDt;
    private Long volume;
    private Long volumePrice;
    private Long startPrice;
    private Long endPrice;
    private Long highPrice;
    private Long lowPrice;
    private Double dailyRange;
    private Double dailyRatio;
    private Long stockTotalCnt;
    private Long marketTotalAmt;
}
