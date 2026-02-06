package com.stock.strategy.service;

import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.BacktestResponse;
import com.stock.strategy.entity.BacktestSimulation;
import com.stock.strategy.enums.SimulationStatus;
import com.stock.strategy.repository.BacktestSimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final BacktestSimulationRepository simulationRepository;
    private final SimulationEngine simulationEngine;

    @Transactional
    public BacktestResponse startBacktest(BacktestRequest request) {
        // 시뮬레이션 엔티티 생성
        BacktestSimulation simulation = BacktestSimulation.builder()
                .strategyName(request.getStrategyName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .initialCapital(request.getInitialCapital())
                .rebalancingPeriod(request.getRebalancingPeriod())
                .tradingFeeRate(request.getTradingFeeRate())
                .taxRate(request.getTaxRate())
                .status(SimulationStatus.PENDING)
                .build();

        simulation = simulationRepository.save(simulation);

        // 비동기로 시뮬레이션 실행
        runSimulationAsync(simulation.getId(), request);

        return BacktestResponse.builder()
                .simulationId(simulation.getId())
                .status(SimulationStatus.PENDING)
                .message("백테스팅이 시작되었습니다.")
                .build();
    }

    @Async
    public void runSimulationAsync(Long simulationId, BacktestRequest request) {
        try {
            log.info("Starting simulation: {}", simulationId);
            simulationEngine.runSimulation(simulationId, request);
            log.info("Simulation completed: {}", simulationId);
        } catch (Exception e) {
            log.error("Simulation failed: {}", simulationId, e);
            updateSimulationStatus(simulationId, SimulationStatus.FAILED);
        }
    }

    @Transactional
    public void updateSimulationStatus(Long simulationId, SimulationStatus status) {
        BacktestSimulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found: " + simulationId));
        simulation.setStatus(status);
        simulationRepository.save(simulation);
    }
}
