package com.stock.strategy.dto;

import com.stock.strategy.entity.BacktestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompareStrategiesResponse {
    private List<BacktestResult> results;
    private Long bestCagrSimulationId;
    private Long bestSharpeSimulationId;
    private Long lowestMddSimulationId;
}
