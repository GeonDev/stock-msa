package com.stock.strategy.strategy;

import com.stock.common.dto.CorpInfoDto;
import com.stock.common.dto.SectorRotationConfig;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.common.utils.DateUtils;
import com.stock.strategy.client.CorpClient;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.dto.PortfolioHolding;
import com.stock.strategy.dto.TradeOrder;
import com.stock.strategy.entity.SectorAnalysis;
import com.stock.strategy.enums.OrderType;
import com.stock.strategy.service.SectorAnalysisService;
import com.stock.strategy.service.SimulationEngine.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorRotationStrategy implements Strategy {

    private final CorpClient corpClient;
    private final PriceClient priceClient;
    private final SectorAnalysisService sectorAnalysisService;

    @Override
    public String getName() {
        return "SectorRotation";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        throw new UnsupportedOperationException("SectorRotationStrategy requires SectorRotationConfig");
    }

    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe, SectorRotationConfig config) {
        List<TradeOrder> orders = new ArrayList<>();
        if (universe.isEmpty()) return orders;

        String dateStr = DateUtils.toLocalDateString(date);

        try {
            // 1. Fetch sector info for universe
            List<CorpInfoDto> corps = corpClient.getCorpsBatch(universe);
            
            Map<String, List<String>> sectorToStocks = corps.stream()
                    .filter(c -> c.getSector() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getSector().name(),
                            Collectors.mapping(CorpInfoDto::getStockCode, Collectors.toList())
                    ));

            // 2. Analyze sectors
            List<SectorAnalysis> sectorScores = sectorAnalysisService.analyzeAndSaveSectors(date, sectorToStocks, dateStr);

            // 3. Select top N sectors
            List<String> topSectors = sectorScores.stream()
                    .sorted(Comparator.comparing(SectorAnalysis::getAvgMomentum12m).reversed())
                    .limit(config.getTopSectorsCount())
                    .map(SectorAnalysis::getSectorName)
                    .collect(Collectors.toList());

            // 4. Select top stocks within top sectors based on momentum
            List<String> targetStocks = new ArrayList<>();
            List<StockIndicatorDto> indicators = priceClient.getIndicatorsByDateBatch(universe, dateStr);
            Map<String, Double> momMap = indicators.stream()
                    .filter(i -> i.getMomentum6m() != null && i.getStockCode() != null)
                    .collect(Collectors.toMap(StockIndicatorDto::getStockCode, i -> i.getMomentum6m().doubleValue(), (i1, i2) -> i1));

            for (String sector : topSectors) {
                List<String> sectorStocks = sectorToStocks.get(sector);
                if (sectorStocks == null) continue;

                List<String> bestInSector = sectorStocks.stream()
                        .filter(momMap::containsKey)
                        .sorted(Comparator.comparingDouble((String s) -> momMap.get(s)).reversed())
                        .limit(config.getStocksPerSector())
                        .collect(Collectors.toList());

                targetStocks.addAll(bestInSector);
            }

            if (targetStocks.isEmpty()) return orders;

            // 5. Sell
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!targetStocks.contains(stockCode)) {
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    orders.add(TradeOrder.builder()
                            .stockCode(stockCode)
                            .orderType(OrderType.SELL)
                            .quantity(holding.getQuantity())
                            .price(holding.getCurrentPrice())
                            .orderDate(date)
                            .build());
                }
            }

            // 6. Buy target stocks equal-weighted
            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetValuePerStock = totalValue.divide(BigDecimal.valueOf(targetStocks.size()), 2, RoundingMode.HALF_UP);

            for (String stockCode : targetStocks) {
                var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                if (priceDto == null || priceDto.getEndPrice() == null) continue;

                BigDecimal currentPrice = priceDto.getEndPrice();
                PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                BigDecimal currentValue = holding != null ? holding.getMarketValue() : BigDecimal.ZERO;

                BigDecimal diff = targetValuePerStock.subtract(currentValue);

                if (diff.abs().compareTo(currentPrice) > 0) {
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        int quantity = diff.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                        if (quantity > 0) {
                            orders.add(TradeOrder.builder()
                                    .stockCode(stockCode)
                                    .orderType(OrderType.BUY)
                                    .quantity(quantity)
                                    .price(currentPrice)
                                    .orderDate(date)
                                    .build());
                        }
                    } else {
                        int quantity = diff.abs().divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                        if (quantity > 0 && holding != null && holding.getQuantity() >= quantity) {
                            orders.add(TradeOrder.builder()
                                    .stockCode(stockCode)
                                    .orderType(OrderType.SELL)
                                    .quantity(quantity)
                                    .price(currentPrice)
                                    .orderDate(date)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to rebalance SectorRotation", e);
        }

        return orders;
    }
}
