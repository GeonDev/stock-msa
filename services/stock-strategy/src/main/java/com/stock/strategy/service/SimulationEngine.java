package com.stock.strategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.dto.StockPriceDto;
import com.stock.common.service.DayOffService;
import com.stock.common.utils.DateUtils;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.BacktestRequest;
import com.stock.strategy.dto.PortfolioHolding;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.entity.BacktestSimulation;
import com.stock.strategy.entity.PortfolioSnapshot;
import com.stock.strategy.entity.TradeHistory;
import com.stock.strategy.enums.RebalancingPeriod;
import com.stock.strategy.enums.SimulationStatus;
import com.stock.strategy.repository.BacktestSimulationRepository;
import com.stock.strategy.repository.PortfolioSnapshotRepository;
import com.stock.strategy.repository.TradeHistoryRepository;
import com.stock.strategy.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationEngine {

    private final BacktestSimulationRepository simulationRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final DayOffService dayOffService;
    private final UniverseFilterService universeFilterService;
    private final StrategyFactory strategyFactory;
    private final PerformanceCalculationService performanceCalculationService;
    private final PriceClient priceClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void runSimulation(Long simulationId, BacktestRequest request) {
        // 시뮬레이션 상태 업데이트
        BacktestSimulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));
        simulation.setStatus(SimulationStatus.RUNNING);
        simulationRepository.save(simulation);

        // 초기 포트폴리오 설정
        Portfolio portfolio = new Portfolio(request.getInitialCapital());
        
        // 전략 가져오기
        Strategy strategy = strategyFactory.getStrategy(request.getStrategyName());

        // 시뮬레이션 실행
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {

            // 휴장일 인경우 다음날 체크
            if (dayOffService.checkIsDayOff(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // 리밸런싱 체크
            if (isRebalancingDate(currentDate, request.getRebalancingPeriod(), request.getStartDate())) {
                // 유니버스 필터링
                List<String> universe = universeFilterService.filter(currentDate, request.getUniverseFilter());
                
                // 전략에 따른 매매 시그널 생성
                List<TradeOrder> orders = strategy.rebalance(currentDate, portfolio, universe);
                
                // 매매 실행
                executeOrders(simulationId, currentDate, orders, portfolio, request.getTradingFeeRate(), request.getTaxRate());
            }

            // 포트폴리오 평가 (현재가 기준)
            updatePortfolioValue(currentDate, portfolio);

            // 스냅샷 저장 (리밸런싱일 또는 월말)
            if (shouldSaveSnapshot(currentDate, request.getRebalancingPeriod())) {
                saveSnapshot(simulationId, currentDate, portfolio);
            }

            currentDate = currentDate.plusDays(1);
        }

        // 성과 지표 계산
        performanceCalculationService.calculateAndSave(simulationId);

        // 시뮬레이션 완료
        simulation.setStatus(SimulationStatus.COMPLETED);
        simulation.setCompletedAt(LocalDateTime.now());
        simulationRepository.save(simulation);
    }

    private boolean isRebalancingDate(LocalDate date, RebalancingPeriod period, LocalDate startDate) {
        if (date.equals(startDate)) {
            return true; // 시작일은 항상 리밸런싱
        }

        return switch (period) {
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek().getValue() == 1; // 월요일
            case MONTHLY -> date.getDayOfMonth() == 1; // 매월 1일
            case QUARTERLY -> date.getDayOfMonth() == 1 && (date.getMonthValue() % 3 == 1); // 분기 첫날
            case YEARLY -> date.getDayOfMonth() == 1 && date.getMonthValue() == 1; // 매년 1월 1일
        };
    }

    private void executeOrders(Long simulationId, LocalDate date, List<TradeOrder> orders, 
                               Portfolio portfolio, BigDecimal feeRate, BigDecimal taxRate) {
        for (TradeOrder order : orders) {
            BigDecimal totalAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal fee = totalAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = BigDecimal.ZERO;

            switch (order.getOrderType()) {
                case BUY -> {
                    BigDecimal totalCost = totalAmount.add(fee);
                    if (portfolio.getCashBalance().compareTo(totalCost) >= 0) {
                        portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalCost));
                        portfolio.addHolding(order.getStockCode(), order.getQuantity(), order.getPrice());
                    }
                }
                case SELL -> {
                    tax = totalAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal netAmount = totalAmount.subtract(fee).subtract(tax);
                    portfolio.setCashBalance(portfolio.getCashBalance().add(netAmount));
                    portfolio.removeHolding(order.getStockCode(), order.getQuantity());
                }
            }

            // 거래 이력 저장
            TradeHistory history = TradeHistory.builder()
                    .simulationId(simulationId)
                    .tradeDate(date)
                    .stockCode(order.getStockCode())
                    .orderType(order.getOrderType().name())
                    .quantity(order.getQuantity())
                    .price(order.getPrice())
                    .fee(fee)
                    .tax(tax)
                    .build();
            tradeHistoryRepository.save(history);
        }
    }

    private void updatePortfolioValue(LocalDate date, Portfolio portfolio) {
        // 보유 종목의 현재가를 조회하여 포트폴리오 가치 업데이트
        String dateStr = DateUtils.toLocalDateString(date);
        
        for (Map.Entry<String, PortfolioHolding> entry : portfolio.getHoldings().entrySet()) {
            String stockCode = entry.getKey();
            PortfolioHolding holding = entry.getValue();
            
            try {
                // 해당 날짜의 주가 조회
                StockPriceDto priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                
                if (priceDto != null && priceDto.getEndPrice() != null) {
                    // 현재가 업데이트
                    holding.setCurrentPrice(priceDto.getEndPrice());
                    // 시장가치 재계산
                    BigDecimal marketValue = priceDto.getEndPrice()
                            .multiply(BigDecimal.valueOf(holding.getQuantity()));
                    holding.setMarketValue(marketValue);
                } else {
                    // 가격 정보가 없으면 이전 가격 유지
                    log.warn("No price data for {} on {}, keeping previous price", stockCode, date);
                }
            } catch (Exception e) {
                // API 호출 실패 시 이전 가격 유지
                log.error("Failed to fetch price for {} on {}: {}", stockCode, date, e.getMessage());
            }
        }
    }

    private boolean shouldSaveSnapshot(LocalDate date, RebalancingPeriod period) {
        // 리밸런싱일 또는 월말에 저장
        return date.getDayOfMonth() == date.lengthOfMonth() || 
               isRebalancingDate(date, period, date);
    }

    private void saveSnapshot(Long simulationId, LocalDate date, Portfolio portfolio) {
        try {
            String holdingsJson = objectMapper.writeValueAsString(portfolio.getHoldings());
            
            PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                    .simulationId(simulationId)
                    .snapshotDate(date)
                    .totalValue(portfolio.getTotalValue())
                    .cashBalance(portfolio.getCashBalance())
                    .holdings(holdingsJson)
                    .build();
            
            snapshotRepository.save(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize holdings", e);
        }
    }

    // 내부 포트폴리오 클래스
    public static class Portfolio {
        private BigDecimal cashBalance;
        private final Map<String, PortfolioHolding> holdings = new HashMap<>();

        public Portfolio(BigDecimal initialCapital) {
            this.cashBalance = initialCapital;
        }

        public BigDecimal getCashBalance() {
            return cashBalance;
        }

        public void setCashBalance(BigDecimal cashBalance) {
            this.cashBalance = cashBalance;
        }

        public Map<String, PortfolioHolding> getHoldings() {
            return holdings;
        }

        public BigDecimal getTotalValue() {
            BigDecimal holdingsValue = holdings.values().stream()
                    .map(PortfolioHolding::getMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return cashBalance.add(holdingsValue);
        }

        public void addHolding(String stockCode, Integer quantity, BigDecimal price) {
            PortfolioHolding holding = holdings.get(stockCode);
            if (holding == null) {
                holding = PortfolioHolding.builder()
                        .stockCode(stockCode)
                        .quantity(quantity)
                        .averagePrice(price)
                        .currentPrice(price)
                        .marketValue(price.multiply(BigDecimal.valueOf(quantity)))
                        .build();
                holdings.put(stockCode, holding);
            } else {
                // 평균 매입가 계산
                BigDecimal totalCost = holding.getAveragePrice().multiply(BigDecimal.valueOf(holding.getQuantity()))
                        .add(price.multiply(BigDecimal.valueOf(quantity)));
                int totalQuantity = holding.getQuantity() + quantity;
                BigDecimal avgPrice = totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
                
                holding.setQuantity(totalQuantity);
                holding.setAveragePrice(avgPrice);
                holding.setCurrentPrice(price);
                holding.setMarketValue(price.multiply(BigDecimal.valueOf(totalQuantity)));
            }
        }

        public void removeHolding(String stockCode, Integer quantity) {
            PortfolioHolding holding = holdings.get(stockCode);
            if (holding != null) {
                int remainingQuantity = holding.getQuantity() - quantity;
                if (remainingQuantity <= 0) {
                    holdings.remove(stockCode);
                } else {
                    holding.setQuantity(remainingQuantity);
                    holding.setMarketValue(holding.getCurrentPrice().multiply(BigDecimal.valueOf(remainingQuantity)));
                }
            }
        }
    }
}
