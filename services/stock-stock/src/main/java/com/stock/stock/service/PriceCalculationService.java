package com.stock.stock.service;

import com.stock.stock.entity.StockMonthlyPrice;
import com.stock.stock.entity.StockPrice;
import com.stock.stock.entity.StockWeeklyPrice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceCalculationService {

    public List<StockWeeklyPrice> calculateWeeklyPrices(List<StockPrice> dailyPrices) {
        if (dailyPrices == null || dailyPrices.isEmpty()) {
            return Collections.emptyList();
        }

        // Group daily prices by week
        Map<String, List<StockPrice>> pricesByWeek = dailyPrices.stream()
                .collect(Collectors.groupingBy(p -> 
                    p.getBasDt().getYear() + "-" + p.getBasDt().get(WeekFields.ISO.weekOfWeekBasedYear())));

        List<StockWeeklyPrice> weeklyPrices = new ArrayList<>();
        for (List<StockPrice> weekPrices : pricesByWeek.values()) {
            if (weekPrices.isEmpty()) continue;

            weekPrices.sort(Comparator.comparing(StockPrice::getBasDt));

            LocalDate startDate = weekPrices.get(0).getBasDt();
            LocalDate endDate = weekPrices.get(weekPrices.size() - 1).getBasDt();
            String stockCode = weekPrices.get(0).getStockCode();
            String marketCode = weekPrices.get(0).getMarketCode();

            int startPrice = weekPrices.get(0).getStartPrice();
            int endPrice = weekPrices.get(weekPrices.size() - 1).getEndPrice();
            int highPrice = weekPrices.stream().mapToInt(StockPrice::getHighPrice).max().orElse(0);
            int lowPrice = weekPrices.stream().mapToInt(StockPrice::getLowPrice).min().orElse(0);
            int volume = weekPrices.stream().mapToInt(StockPrice::getVolume).sum();
            long volumePrice = weekPrices.stream().mapToLong(StockPrice::getVolumePrice).sum();
            long stockTotalCnt = weekPrices.get(weekPrices.size() - 1).getStockTotalCnt();
            long marketTotalAmt = weekPrices.get(weekPrices.size() - 1).getMarketTotalAmt();

            weeklyPrices.add(StockWeeklyPrice.builder()
                    .stockCode(stockCode)
                    .marketCode(marketCode)
                    .startDate(startDate)
                    .endDate(endDate)
                    .startPrice(startPrice)
                    .endPrice(endPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .volume(volume)
                    .volumePrice(volumePrice)
                    .stockTotalCnt(stockTotalCnt)
                    .marketTotalAmt(marketTotalAmt)
                    .build());
        }

        return weeklyPrices;
    }

    public List<StockMonthlyPrice> calculateMonthlyPrices(List<StockPrice> dailyPrices) {
        if (dailyPrices == null || dailyPrices.isEmpty()) {
            return Collections.emptyList();
        }

        // Group daily prices by month
        Map<String, List<StockPrice>> pricesByMonth = dailyPrices.stream()
                .collect(Collectors.groupingBy(p -> 
                    p.getBasDt().getYear() + "-" + p.getBasDt().getMonthValue()));

        List<StockMonthlyPrice> monthlyPrices = new ArrayList<>();
        for (List<StockPrice> monthPrices : pricesByMonth.values()) {
            if (monthPrices.isEmpty()) continue;

            monthPrices.sort(Comparator.comparing(StockPrice::getBasDt));

            LocalDate startDate = monthPrices.get(0).getBasDt();
            LocalDate endDate = monthPrices.get(monthPrices.size() - 1).getBasDt();
            String stockCode = monthPrices.get(0).getStockCode();
            String marketCode = monthPrices.get(0).getMarketCode();

            int startPrice = monthPrices.get(0).getStartPrice();
            int endPrice = monthPrices.get(monthPrices.size() - 1).getEndPrice();
            int highPrice = monthPrices.stream().mapToInt(StockPrice::getHighPrice).max().orElse(0);
            int lowPrice = monthPrices.stream().mapToInt(StockPrice::getLowPrice).min().orElse(0);
            int volume = monthPrices.stream().mapToInt(StockPrice::getVolume).sum();
            long volumePrice = monthPrices.stream().mapToLong(StockPrice::getVolumePrice).sum();
            long stockTotalCnt = monthPrices.get(monthPrices.size() - 1).getStockTotalCnt();
            long marketTotalAmt = monthPrices.get(monthPrices.size() - 1).getMarketTotalAmt();

            monthlyPrices.add(StockMonthlyPrice.builder()
                    .stockCode(stockCode)
                    .marketCode(marketCode)
                    .startDate(startDate)
                    .endDate(endDate)
                    .startPrice(startPrice)
                    .endPrice(endPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .volume(volume)
                    .volumePrice(volumePrice)
                    .stockTotalCnt(stockTotalCnt)
                    .marketTotalAmt(marketTotalAmt)
                    .build());
        }

        return monthlyPrices;
    }
}
