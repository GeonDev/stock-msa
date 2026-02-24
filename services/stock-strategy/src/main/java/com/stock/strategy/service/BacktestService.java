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

import com.stock.common.dto.ValueStrategyConfig;
import com.stock.strategy.dto.CompareStrategiesResponse;
import com.stock.strategy.dto.GridSearchRequest;
import com.stock.strategy.entity.BacktestResult;
import com.stock.strategy.repository.BacktestResultRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final BacktestSimulationRepository simulationRepository;
    private final BacktestResultRepository resultRepository;
    private final SimulationEngine simulationEngine;

    @Transactional
    public BacktestResponse startBacktest(BacktestRequest request) {
        return startBacktestInternal(request, false);
    }

    private BacktestResponse startBacktestInternal(BacktestRequest request, boolean isOptimized) {
        // 시뮬레이션 엔티티 생성
        BacktestSimulation simulation = BacktestSimulation.builder()
                .strategyName(request.getStrategyType().getCode())
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
        runSimulationAsync(simulation.getId(), request, isOptimized);

        return BacktestResponse.builder()
                .simulationId(simulation.getId())
                .status(SimulationStatus.PENDING)
                .message("백테스팅이 시작되었습니다.")
                .build();
    }

    @Async
    public void runSimulationAsync(Long simulationId, BacktestRequest request, boolean isOptimized) {
        try {
            log.info("Starting simulation: {}", simulationId);
            simulationEngine.runSimulation(simulationId, request);
            
            // result update
            resultRepository.findBySimulationId(simulationId).ifPresent(result -> {
                boolean changed = false;
                if (isOptimized) {
                    result.setIsOptimized(true);
                    changed = true;
                }
                if (request.getSlippageType() != null) {
                    result.setSlippageType(request.getSlippageType().name());
                    changed = true;
                }
                if (changed) {
                    resultRepository.save(result);
                }
            });
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

    public CompareStrategiesResponse compareStrategies(List<Long> resultIds) {
        List<BacktestResult> results = resultRepository.findAllById(resultIds);
        
        Long bestCagrId = results.stream()
                .max(Comparator.comparing(BacktestResult::getCagr, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(BacktestResult::getSimulationId)
                .orElse(null);

        Long bestSharpeId = results.stream()
                .max(Comparator.comparing(BacktestResult::getSharpeRatio, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(BacktestResult::getSimulationId)
                .orElse(null);

        Long lowestMddId = results.stream()
                .min(Comparator.comparing(BacktestResult::getMdd, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(BacktestResult::getSimulationId)
                .orElse(null);

        return CompareStrategiesResponse.builder()
                .results(results)
                .bestCagrSimulationId(bestCagrId)
                .bestSharpeSimulationId(bestSharpeId)
                .lowestMddSimulationId(lowestMddId)
                .build();
    }

    @Transactional
    public BacktestResponse startGridSearch(GridSearchRequest request) {
        List<BacktestRequest> combinations = generateCombinations(request);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // 병렬로 여러 시뮬레이션 실행 (비동기)
        for (BacktestRequest comboRequest : combinations) {
            startBacktestInternal(comboRequest, true);
        }

        return BacktestResponse.builder()
                .status(SimulationStatus.PENDING)
                .message(combinations.size() + "개의 조합에 대한 그리드 서치 백테스팅이 시작되었습니다.")
                .build();
    }

    private List<BacktestRequest> generateCombinations(GridSearchRequest request) {
        List<BacktestRequest> list = new ArrayList<>();
        
        int minN = request.getMinTopN() != null ? request.getMinTopN() : 20;
        int maxN = request.getMaxTopN() != null ? request.getMaxTopN() : 20;
        int stepN = request.getStepTopN() != null ? request.getStepTopN() : 10;
        double wStep = request.getWeightStep() != null ? request.getWeightStep() : 0.1;

        for (int topN = minN; topN <= maxN; topN += stepN) {
            for (double pbrW = 0.0; pbrW <= 1.0; pbrW += wStep) {
                for (double roeW = 0.0; roeW <= (1.0 - pbrW); roeW += wStep) {
                    double perW = 1.0 - pbrW - roeW;
                    // 부동소수점 오차 보정
                    if (perW < -0.0001 || perW > 1.0001) continue;
                    
                    if (perW < 0) perW = 0.0;
                    
                    ValueStrategyConfig config = ValueStrategyConfig.builder()
                            .topN(topN)
                            .pbrWeight(BigDecimal.valueOf(pbrW))
                            .roeWeight(BigDecimal.valueOf(roeW))
                            .perWeight(BigDecimal.valueOf(perW))
                            .build();

                    BacktestRequest comboRequest = copyRequest(request.getBaseRequest());
                    comboRequest.setValueStrategyConfig(config);
                    list.add(comboRequest);
                }
            }
        }
        return list;
    }

    private BacktestRequest copyRequest(BacktestRequest original) {
        return BacktestRequest.builder()
                .strategyType(original.getStrategyType())
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .initialCapital(original.getInitialCapital())
                .rebalancingPeriod(original.getRebalancingPeriod())
                .tradingFeeRate(original.getTradingFeeRate())
                .taxRate(original.getTaxRate())
                .universeFilter(original.getUniverseFilter())
                .slippageType(original.getSlippageType())
                .fixedSlippageRate(original.getFixedSlippageRate())
                .maxWeightPerStock(original.getMaxWeightPerStock())
                .maxVolumeRatio(original.getMaxVolumeRatio())
                .build();
    }
}
