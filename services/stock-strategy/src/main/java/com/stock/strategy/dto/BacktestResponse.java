package com.stock.strategy.dto;

import com.stock.strategy.enums.SimulationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResponse {
    private Long simulationId;
    private SimulationStatus status;
    private String message;
}
