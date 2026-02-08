package com.stock.strategy.service;

import com.stock.strategy.entity.BacktestResult;
import com.stock.strategy.entity.BacktestSimulation;
import com.stock.strategy.entity.PortfolioSnapshot;
import com.stock.strategy.entity.TradeHistory;
import com.stock.strategy.repository.BacktestResultRepository;
import com.stock.strategy.repository.BacktestSimulationRepository;
import com.stock.strategy.repository.PortfolioSnapshotRepository;
import com.stock.strategy.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceCalculationService {

    private final BacktestSimulationRepository simulationRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final BacktestResultRepository resultRepository;

    @Transactional
    public void calculateAndSave(Long simulationId) {
        BacktestSimulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));

        List<PortfolioSnapshot> snapshots = snapshotRepository.findBySimulationIdOrderBySnapshotDateAsc(simulationId);
        List<TradeHistory> trades = tradeHistoryRepository.findBySimulationIdOrderByTradeDateAsc(simulationId);

        if (snapshots.isEmpty()) {
            log.warn("No snapshots found for simulation: {}", simulationId);
            return;
        }

        // 최종 가치
        BigDecimal finalValue = snapshots.get(snapshots.size() - 1).getTotalValue();
        BigDecimal initialCapital = simulation.getInitialCapital();

        // 총 수익률
        BigDecimal totalReturn = finalValue.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // CAGR 계산
        long days = ChronoUnit.DAYS.between(simulation.getStartDate(), simulation.getEndDate());
        double years = days / 365.0;
        double cagr = (Math.pow(finalValue.divide(initialCapital, 10, RoundingMode.HALF_UP).doubleValue(), 1.0 / years) - 1) * 100;

        // MDD 계산
        BigDecimal mdd = calculateMDD(snapshots);

        // 샤프 비율 계산 (간단 버전)
        BigDecimal sharpeRatio = calculateSharpeRatio(snapshots);

        // 변동성 계산
        BigDecimal volatility = calculateVolatility(snapshots);

        // 승률 계산
        int totalTrades = trades.size() / 2; // 매수/매도 쌍
        int profitableTrades = calculateProfitableTrades(trades);
        BigDecimal winRate = totalTrades > 0 
                ? BigDecimal.valueOf(profitableTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // 결과 저장
        BacktestResult result = BacktestResult.builder()
                .simulationId(simulationId)
                .finalValue(finalValue)
                .totalReturn(totalReturn)
                .cagr(BigDecimal.valueOf(cagr).setScale(4, RoundingMode.HALF_UP))
                .mdd(mdd)
                .sharpeRatio(sharpeRatio)
                .volatility(volatility)
                .winRate(winRate)
                .totalTrades(totalTrades)
                .profitableTrades(profitableTrades)
                .build();

        resultRepository.save(result);
    }

    private BigDecimal calculateMDD(List<PortfolioSnapshot> snapshots) {
        BigDecimal maxValue = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (PortfolioSnapshot snapshot : snapshots) {
            BigDecimal currentValue = snapshot.getTotalValue();
            if (currentValue.compareTo(maxValue) > 0) {
                maxValue = currentValue;
            }

            BigDecimal drawdown = maxValue.subtract(currentValue)
                    .divide(maxValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    private BigDecimal calculateSharpeRatio(List<PortfolioSnapshot> snapshots) {
        // 간단한 샤프 비율 계산 (무위험 수익률 0 가정)
        if (snapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            BigDecimal prevValue = snapshots.get(i - 1).getTotalValue();
            BigDecimal currValue = snapshots.get(i).getTotalValue();
            BigDecimal dailyReturn = currValue.subtract(prevValue).divide(prevValue, 6, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return avgReturn.divide(stdDev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    private BigDecimal calculateVolatility(List<PortfolioSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            BigDecimal prevValue = snapshots.get(i - 1).getTotalValue();
            BigDecimal currValue = snapshots.get(i).getTotalValue();
            BigDecimal dailyReturn = currValue.subtract(prevValue).divide(prevValue, 6, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()) * Math.sqrt(252) * 100)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private int calculateProfitableTrades(List<TradeHistory> trades) {
        // 종목별로 매수-매도 쌍을 추적하여 수익 여부 판단
        Map<String, List<TradeHistory>> tradesByStock = trades.stream()
                .collect(Collectors.groupingBy(TradeHistory::getStockCode));

        int profitableCount = 0;

        for (Map.Entry<String, List<TradeHistory>> entry : tradesByStock.entrySet()) {
            List<TradeHistory> stockTrades = entry.getValue();
            
            // 매수/매도를 시간순으로 정렬
            stockTrades.sort(Comparator.comparing(TradeHistory::getTradeDate));

            // FIFO 방식으로 매수-매도 매칭
            Queue<TradeHistory> buyQueue = new LinkedList<>();
            
            for (TradeHistory trade : stockTrades) {
                if ("BUY".equals(trade.getOrderType())) {
                    buyQueue.offer(trade);
                } else if ("SELL".equals(trade.getOrderType())) {
                    // 매도 시 가장 오래된 매수와 매칭
                    if (!buyQueue.isEmpty()) {
                        TradeHistory buyTrade = buyQueue.poll();
                        
                        // 수익 계산 (매도가 - 매수가)
                        BigDecimal profit = trade.getPrice().subtract(buyTrade.getPrice())
                                .multiply(BigDecimal.valueOf(Math.min(trade.getQuantity(), buyTrade.getQuantity())));
                        
                        if (profit.compareTo(BigDecimal.ZERO) > 0) {
                            profitableCount++;
                        }
                    }
                }
            }
        }

        return profitableCount;
    }
}
