package com.stock.strategy.dto;

import com.stock.strategy.enums.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "백테스팅 실행 응답 정보")
public class BacktestResponse {
    @Schema(description = "시뮬레이션 고유 ID", example = "100")
    private Long simulationId;

    @Schema(description = "시뮬레이션 실행 상태", example = "PENDING")
    private SimulationStatus status;

    @Schema(description = "응답 메시지", example = "Backtesting job has been submitted.")
    private String message;
}
