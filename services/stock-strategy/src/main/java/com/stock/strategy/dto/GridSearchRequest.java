package com.stock.strategy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "백테스팅 그리드 서치 요청")
public class GridSearchRequest {
    
    @Valid
    @NotNull(message = "기본 백테스팅 설정은 필수입니다")
    @Schema(description = "기본 백테스팅 설정 (기간, 자본금, 수수료 등)")
    private BacktestRequest baseRequest;

    @Schema(description = "최소 선정 종목 수", example = "10")
    private Integer minTopN;

    @Schema(description = "최대 선정 종목 수", example = "30")
    private Integer maxTopN;

    @Schema(description = "종목 수 증가 스텝", example = "10")
    private Integer stepTopN;

    @Schema(description = "가중치 탐색 스텝 (예: 0.1 단위 탐색)", example = "0.1")
    private Double weightStep;
}
