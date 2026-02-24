package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "동적 자산배분 및 리스크 패리티 전략 설정")
public class AssetAllocationConfig {
    
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Builder.Default
    @Schema(description = "목표 변동성 (Target Volatility)", example = "0.1")
    private BigDecimal targetVolatility = new BigDecimal("0.10");

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Builder.Default
    @Schema(description = "최대 위험자산(주식) 비중", example = "1.0")
    private BigDecimal maxRiskAssetWeight = BigDecimal.ONE;
    
    @Builder.Default
    @Schema(description = "듀얼 모멘텀 사용 여부", example = "true")
    private boolean useDualMomentum = true;
}
