package com.stock.strategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.dto.StockPriceDto;
import com.stock.common.service.DayOffService;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.PortfolioHolding;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.entity.BacktestResult;
import com.stock.strategy.entity.PortfolioSnapshot;
import com.stock.strategy.enums.OrderType;
import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.SlippageType;
import com.stock.strategy.service.slippage.SlippageModel;
import com.stock.strategy.service.slippage.SlippageModelFactory;
import com.stock.strategy.repository.PortfolioSnapshotRepository;
import com.stock.strategy.strategy.Strategy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationEngine {

    private final DayOffService dayOffService;
    private final UniverseFilterService universeFilterService;
    private final PriceClient priceClient;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public BacktestResult runSimulation(Long simulationId, BacktestRequest request, Strategy strategy) {
        Portfolio portfolio = new Portfolio(request.getInitialCapital());
        LocalDate currentDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        SlippageModel slippageModel = SlippageModelFactory.create(
                request.getSlippageType() != null ? request.getSlippageType() : SlippageType.NONE,
                request.getFixedSlippageRate()
        );

        while (!currentDate.isAfter(endDate)) {
            // 휴장일 인경우 다음날 체크
            if (dayOffService.checkIsDayOff(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            log.info("Processing backtest date: {}", currentDate);

            // 리밸런싱 체크
            if (isRebalancingDate(currentDate, request.getRebalancingPeriod(), request.getStartDate())) {
                log.info("Rebalancing on: {}", currentDate);
                // 유니버스 필터링
                List<String> universe = universeFilterService.filter(currentDate, request.getUniverseFilter());
                
                // 전략 실행
                List<TradeOrder> orders = strategy.rebalance(currentDate, portfolio, universe, request);
                
                // 주문 실행
                executeOrders(orders, portfolio, slippageModel, request.getTradingFeeRate(), request.getTaxRate());
            }

            // 일일 성과 계산 (현금 + 주식)
            calculateDailyReturn(currentDate, portfolio);
            
            // 포트폴리오 스냅샷 저장
            saveSnapshot(currentDate, portfolio, simulationId);

            currentDate = currentDate.plusDays(1);
        }

        // 최종 결과 계산
        return calculateBacktestResult(simulationId, request, portfolio);
    }

    private boolean isRebalancingDate(LocalDate date, RebalancingPeriod period, LocalDate startDate) {
        if (date.equals(startDate)) {
            return true; // 시작일은 항상 리밸런싱
        }

        return switch (period) {
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek().getValue() == 1; // 월요일
            case MONTHLY -> date.getDayOfMonth() == 1; // 매월 1일
            case QUARTERLY -> date.getDayOfMonth() == 1 && (date.getMonthValue() % 3 == 1);
            case YEARLY -> date.getDayOfYear() == 1;
        };
    }

    private void executeOrders(List<TradeOrder> orders, Portfolio portfolio, SlippageModel slippageModel, BigDecimal feeRate, BigDecimal taxRate) {
        // 1. 매도 먼저 처리
        orders.stream()
                .filter(o -> o.getOrderType() == OrderType.SELL)
                .forEach(o -> processOrder(o, portfolio, slippageModel, feeRate, taxRate));

        // 2. 매수 처리
        orders.stream()
                .filter(o -> o.getOrderType() == OrderType.BUY)
                .forEach(o -> processOrder(o, portfolio, slippageModel, feeRate, taxRate));
    }

    private void processOrder(TradeOrder order, Portfolio portfolio, SlippageModel slippageModel, BigDecimal feeRate, BigDecimal taxRate) {
        BigDecimal execPrice = slippageModel.calculateExecutionPrice(order.getPrice(), order.getQuantity(), order.getOrderType());
        BigDecimal totalAmount = execPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        
        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal fee = totalAmount.multiply(feeRate);
            BigDecimal totalCost = totalAmount.add(fee);
            
            if (portfolio.getCashBalance().compareTo(totalCost) >= 0) {
                portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalCost));
                PortfolioHolding holding = portfolio.getHoldings().getOrDefault(order.getStockCode(), new PortfolioHolding());
                holding.setStockCode(order.getStockCode());
                holding.setQuantity(holding.getQuantity() + order.getQuantity());
                holding.setAveragePrice(totalAmount.divide(BigDecimal.valueOf(holding.getQuantity()), 2, RoundingMode.HALF_UP));
                portfolio.getHoldings().put(order.getStockCode(), holding);
                portfolio.getTrades().add(order);
            }
        } else {
            PortfolioHolding holding = portfolio.getHoldings().get(order.getStockCode());
            if (holding != null && holding.getQuantity() >= order.getQuantity()) {
                BigDecimal fee = totalAmount.multiply(feeRate);
                BigDecimal tax = totalAmount.multiply(taxRate);
                BigDecimal netProceeds = totalAmount.subtract(fee).subtract(tax);
                
                portfolio.setCashBalance(portfolio.getCashBalance().add(netProceeds));
                int remainingQuantity = holding.getQuantity() - order.getQuantity();
                if (remainingQuantity == 0) {
                    portfolio.getHoldings().remove(order.getStockCode());
                } else {
                    holding.setQuantity(remainingQuantity);
                }
                portfolio.getTrades().add(order);
            }
        }
    }

    private void calculateDailyReturn(LocalDate date, Portfolio portfolio) {
        BigDecimal stockValue = BigDecimal.ZERO;
        String dateStr = date.toString();

        for (PortfolioHolding holding : portfolio.getHoldings().values()) {
            var priceDto = priceClient.getPriceByDate(holding.getStockCode(), dateStr);
            if (priceDto != null && priceDto.getEndPrice() != null) {
                holding.setCurrentPrice(priceDto.getEndPrice());
                holding.setMarketValue(priceDto.getEndPrice().multiply(BigDecimal.valueOf(holding.getQuantity())));
            }
            stockValue = stockValue.add(holding.getMarketValue());
        }
        portfolio.setTotalValue(portfolio.getCashBalance().add(stockValue));
    }

    private void saveSnapshot(LocalDate date, Portfolio portfolio, Long simulationId) {
        try {
            PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                    .simulationId(simulationId)
                    .snapshotDate(date)
                    .cashBalance(portfolio.getCashBalance())
                    .totalValue(portfolio.getTotalValue())
                    .holdings(objectMapper.writeValueAsString(portfolio.getHoldings()))
                    .build();
            snapshotRepository.save(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to save snapshot", e);
        }
    }

    private BacktestResult calculateBacktestResult(Long simulationId, BacktestRequest request, Portfolio portfolio) {
        BigDecimal totalReturn = portfolio.getTotalValue().subtract(request.getInitialCapital())
                .divide(request.getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return BacktestResult.builder()
                .simulationId(simulationId)
                .finalValue(portfolio.getTotalValue())
                .totalReturn(totalReturn)
                .totalTrades(portfolio.getTrades().size())
                .profitableTrades(0) // Logic needed to calculate this
                .cagr(BigDecimal.ZERO) // Logic needed
                .mdd(BigDecimal.ZERO) // Logic needed
                .build();
    }

    @Data
    public static class Portfolio {
        private BigDecimal cashBalance;
        private BigDecimal totalValue;
        private Map<String, PortfolioHolding> holdings = new HashMap<>();
        private List<TradeOrder> trades = new ArrayList<>();

        public Portfolio(BigDecimal initialCapital) {
            this.cashBalance = initialCapital;
            this.totalValue = initialCapital;
        }
    }
}
