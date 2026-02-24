package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 요약 정보 DTO")
public class DashboardSummaryDto {

    @Schema(description = "YTD 평균 전략 수익률", example = "14.2")
    private double avgReturnYtd;

    @Schema(description = "전체 유니버스 종목 수", example = "2694")
    private long totalUniverseCount;

    @Schema(description = "재무 데이터 검증 통과율 (%)", example = "93.7")
    private double dataVerificationRate;

    @Schema(description = "최근 백테스트 결과 리스트")
    private List<BacktestSummaryDto> topStrategies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestSummaryDto {
        private String name;
        private double cagr;
        private double mdd;
    }
}
