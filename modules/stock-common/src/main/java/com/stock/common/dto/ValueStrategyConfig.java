package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "가치 투자 전략 설정")
public class ValueStrategyConfig {
    
    @Min(value = 1, message = "선정 종목 수는 1개 이상이어야 합니다")
    @Builder.Default
    @Schema(description = "선정할 상위 종목 수", example = "20")
    private Integer topN = 20;
    
    @DecimalMin(value = "0.0", message = "PER 가중치는 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "PER 가중치는 1 이하여야 합니다")
    @Builder.Default
    @Schema(description = "PER 가중치 (0.0 ~ 1.0)", example = "0.3")
    private BigDecimal perWeight = new BigDecimal("0.3");
    
    @DecimalMin(value = "0.0", message = "PBR 가중치는 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "PBR 가중치는 1 이하여야 합니다")
    @Builder.Default
    @Schema(description = "PBR 가중치 (0.0 ~ 1.0)", example = "0.3")
    private BigDecimal pbrWeight = new BigDecimal("0.3");
    
    @DecimalMin(value = "0.0", message = "ROE 가중치는 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "ROE 가중치는 1 이하여야 합니다")
    @Builder.Default
    @Schema(description = "ROE 가중치 (0.0 ~ 1.0)", example = "0.4")
    private BigDecimal roeWeight = new BigDecimal("0.4");
    
    /**
     * 가중치 합계 검증
     */
    public void validate() {
        BigDecimal sum = perWeight.add(pbrWeight).add(roeWeight);
        BigDecimal tolerance = new BigDecimal("0.0001");
        
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                String.format("가중치 합계는 1.0이어야 합니다. 현재 합계: %s", sum));
        }
    }
}
