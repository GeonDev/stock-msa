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
    private Integer volume;
    private Long volumePrice;
    private Integer startPrice;
    private Integer endPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Double dailyRange;
    private Double dailyRatio;
    private Long stockTotalCnt;
    private Long marketTotalAmt;
}
