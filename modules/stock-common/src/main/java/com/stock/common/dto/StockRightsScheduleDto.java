package com.stock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockRightsScheduleDto {
    private String stockCode;
    private String eventType;
    private String referenceDate; // 기준일
    private String periodStartDate;
    private String periodEndDate;
}
