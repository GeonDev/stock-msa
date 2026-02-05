package com.stock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal volume;
    private BigDecimal volumePrice;
    private BigDecimal startPrice;
    private BigDecimal endPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal dailyRange;
    private BigDecimal dailyRatio;
    private BigDecimal stockTotalCnt;
    private BigDecimal marketTotalAmt;
}
