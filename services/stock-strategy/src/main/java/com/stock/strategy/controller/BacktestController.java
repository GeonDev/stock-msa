package com.stock.strategy.controller;

import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.BacktestResponse;
import com.stock.strategy.entity.BacktestResult;
import com.stock.strategy.entity.PortfolioSnapshot;
import com.stock.strategy.repository.BacktestResultRepository;
import com.stock.strategy.repository.PortfolioSnapshotRepository;
import com.stock.strategy.service.BacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import com.stock.strategy.dto.CompareStrategiesResponse;
import com.stock.strategy.dto.GridSearchRequest;
import com.stock.common.dto.DashboardSummaryDto;
import java.util.Arrays;
import java.util.stream.Collectors;

@Tag(name = "Backtest", description = "백테스팅 API")
@RestController
@RequestMapping("/api/v1/strategy/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;
    private final BacktestResultRepository resultRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @Operation(summary = "백테스팅 시작", description = "새로운 백테스팅 시뮬레이션을 시작합니다")
    @PostMapping
    public ResponseEntity<BacktestResponse> startBacktest(@Valid @RequestBody BacktestRequest request) {
        BacktestResponse response = backtestService.startBacktest(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전략 비교", description = "여러 백테스트 결과를 비교합니다")
    @GetMapping("/compare")
    public ResponseEntity<CompareStrategiesResponse> compareStrategies(@RequestParam String resultIds) {
        List<Long> ids = Arrays.stream(resultIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        CompareStrategiesResponse response = backtestService.compareStrategies(ids);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "그리드 서치 최적화", description = "최적의 파라미터를 탐색합니다")
    @PostMapping("/optimize")
    public ResponseEntity<BacktestResponse> optimizeStrategies(@Valid @RequestBody GridSearchRequest request) {
        BacktestResponse response = backtestService.startGridSearch(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대시보드 요약 정보 조회", description = "메인 대시보드에 표시할 요약 정보를 조회합니다")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(backtestService.getDashboardSummary());
    }

    @Operation(summary = "백테스팅 결과 조회", description = "완료된 백테스팅의 성과 지표를 조회합니다")
    @GetMapping("/{simulationId}/result")
    public ResponseEntity<BacktestResult> getBacktestResult(@PathVariable Long simulationId) {
        BacktestResult result = resultRepository.findBySimulationId(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Result not found for simulation: " + simulationId));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "포트폴리오 스냅샷 조회", description = "백테스팅 기간 동안의 포트폴리오 스냅샷을 조회합니다")
    @GetMapping("/{simulationId}/snapshots")
    public ResponseEntity<List<PortfolioSnapshot>> getSnapshots(
            @PathVariable Long simulationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PortfolioSnapshot> snapshots;
        if (startDate != null && endDate != null) {
            snapshots = snapshotRepository.findBySimulationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                    simulationId, startDate, endDate);
        } else {
            snapshots = snapshotRepository.findBySimulationIdOrderBySnapshotDateAsc(simulationId);
        }
        
        return ResponseEntity.ok(snapshots);
    }
}
