package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "주식 권리 일정 정보 DTO")
public class StockRightsScheduleDto {
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "이벤트 유형 (배당, 권리락 등)", example = "배당")
    private String eventType;

    @Schema(description = "기준일", example = "20231231")
    private String referenceDate; // 기준일

    @Schema(description = "기간 시작일", example = "20240101")
    private String periodStartDate;

    @Schema(description = "기간 종료일", example = "20240115")
    private String periodEndDate;
}
