package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "섹터 로테이션 전략 설정")
public class SectorRotationConfig {
    
    @Min(value = 1, message = "선정할 섹터 수는 1개 이상이어야 합니다")
    @Builder.Default
    @Schema(description = "상대 강도가 높은 상위 N개 섹터 선택", example = "3")
    private Integer topSectorsCount = 3;

    @Min(value = 1, message = "각 섹터별 종목 수는 1개 이상이어야 합니다")
    @Builder.Default
    @Schema(description = "선택된 섹터 내 매수할 우량주 수", example = "5")
    private Integer stocksPerSector = 5;
}
