package com.stock.batch.stock.service;

import com.stock.batch.stock.entity.StockMonthlyPrice;
import com.stock.batch.stock.entity.StockPrice;
import com.stock.batch.stock.entity.StockWeeklyPrice;
import com.stock.common.enums.StockMarket;
import com.stock.batch.stock.repository.StockMonthlyPriceRepository;
import com.stock.batch.stock.repository.StockPriceRepository;
import com.stock.batch.stock.repository.StockWeeklyPriceRepository;
import com.stock.common.service.DayOffService;
import com.stock.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final StockPriceRepository stockPriceRepository;
    private final StockWeeklyPriceRepository stockWeeklyPriceRepository;
    private final StockMonthlyPriceRepository stockMonthlyPriceRepository;
    private final StockService stockService;
    private final PriceCalculationService priceCalculationService;
    private final DayOffService dayOffService;

    @Transactional
    public void recoverStockPrices(LocalDate startDate, LocalDate endDate) {
        // Phase 1: Re-populate Daily Prices (Upsert)
        log.info("[Recovery] Re-fetching daily prices from {} to {}", startDate, endDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (dayOffService.checkIsDayOff(date)) {
                log.info("[Recovery] Skipping holiday: {}", date);
                continue;
            }
            try {
                String dateStr = DateUtils.toLocalDateString(date);
                log.info("[Recovery] Fetching and saving daily prices for {}", dateStr);
                List<StockPrice> kospiPrices = stockService.getStockPrice(StockMarket.KOSPI, dateStr);
                List<StockPrice> kosdaqPrices = stockService.getStockPrice(StockMarket.KOSDAQ, dateStr);

                List<StockPrice> fetchedPrices = new ArrayList<>();
                fetchedPrices.addAll(kospiPrices);
                fetchedPrices.addAll(kosdaqPrices);

                if (!fetchedPrices.isEmpty()) {
                    List<StockPrice> existingPrices = stockPriceRepository.findByBasDt(date);
                    Map<String, StockPrice> existingMap = existingPrices.stream()
                            .collect(Collectors.toMap(StockPrice::getStockCode, p -> p, (p1, p2) -> p1));

                    List<StockPrice> toSave = new ArrayList<>();
                    for (StockPrice newPrice : fetchedPrices) {
                        if (existingMap.containsKey(newPrice.getStockCode())) {
                            StockPrice existing = existingMap.get(newPrice.getStockCode());
                            updateStockPrice(existing, newPrice);
                            toSave.add(existing);
                        } else {
                            toSave.add(newPrice);
                        }
                    }
                    stockPriceRepository.saveAll(toSave);
                }
            } catch (Exception e) {
                log.error("[Recovery] Failed to fetch price data for date: {}", date, e);
            }
        }
        log.info("[Recovery] Daily price re-fetching and saving complete.");

        // Phase 2: Recalculate Aggregates (Upsert)
        log.info("[Recovery] Starting recalculation of weekly and monthly prices.");

        List<String> affectedStockCodes = stockPriceRepository.findDistinctStockCodeByBasDtBetween(startDate, endDate);
        if (affectedStockCodes.isEmpty()) {
            log.info("[Recovery] No stock prices were updated, skipping aggregate recalculation.");
            return;
        }

        // Define recalculation ranges
        LocalDate recalcWeeklyStart = startDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate recalcWeeklyEnd = endDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate recalcMonthlyStart = startDate.withDayOfMonth(1);
        LocalDate recalcMonthlyEnd = endDate.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

        // Fetch all necessary daily prices from DB for the recalculation
        LocalDate overallRecalcStart = recalcWeeklyStart.isBefore(recalcMonthlyStart) ? recalcWeeklyStart : recalcMonthlyStart;
        LocalDate overallRecalcEnd = recalcWeeklyEnd.isAfter(recalcMonthlyEnd) ? recalcWeeklyEnd : recalcMonthlyEnd;
        log.info("[Recovery] Fetching daily prices from DB for recalculation from {} to {}.", overallRecalcStart, overallRecalcEnd);
        List<StockPrice> pricesForRecalc = stockPriceRepository.findByStockCodeInAndBasDtBetween(affectedStockCodes, overallRecalcStart, overallRecalcEnd);

        // Group prices by stock for efficient calculation
        Map<String, List<StockPrice>> pricesByStock = pricesForRecalc.stream()
                .collect(Collectors.groupingBy(StockPrice::getStockCode));

        List<StockWeeklyPrice> calculatedWeeklyPrices = new ArrayList<>();
        List<StockMonthlyPrice> calculatedMonthlyPrices = new ArrayList<>();

        // Recalculate aggregates
        for (List<StockPrice> dailyPrices : pricesByStock.values()) {
            calculatedWeeklyPrices.addAll(priceCalculationService.calculateWeeklyPrices(dailyPrices));
            calculatedMonthlyPrices.addAll(priceCalculationService.calculateMonthlyPrices(dailyPrices));
        }

        // Upsert Weekly Prices
        if (!calculatedWeeklyPrices.isEmpty()) {
            List<StockWeeklyPrice> existingWeekly = stockWeeklyPriceRepository.findByStockCodeInAndEndDateBetween(affectedStockCodes, recalcWeeklyStart, recalcWeeklyEnd);
            // Key: StockCode + StartDate + EndDate (Assuming unique combination)
            Map<String, StockWeeklyPrice> existingWeeklyMap = existingWeekly.stream()
                    .collect(Collectors.toMap(
                            p -> p.getStockCode() + "_" + p.getStartDate() + "_" + p.getEndDate(),
                            p -> p, (p1, p2) -> p1
                    ));

            List<StockWeeklyPrice> weeklyToSave = new ArrayList<>();
            for (StockWeeklyPrice newPrice : calculatedWeeklyPrices) {
                String key = newPrice.getStockCode() + "_" + newPrice.getStartDate() + "_" + newPrice.getEndDate();
                if (existingWeeklyMap.containsKey(key)) {
                    StockWeeklyPrice existing = existingWeeklyMap.get(key);
                    updateWeeklyPrice(existing, newPrice);
                    weeklyToSave.add(existing);
                } else {
                    weeklyToSave.add(newPrice);
                }
            }
            stockWeeklyPriceRepository.saveAll(weeklyToSave);
        }

        // Upsert Monthly Prices
        if (!calculatedMonthlyPrices.isEmpty()) {
            List<StockMonthlyPrice> existingMonthly = stockMonthlyPriceRepository.findByStockCodeInAndEndDateBetween(affectedStockCodes, recalcMonthlyStart, recalcMonthlyEnd);
            Map<String, StockMonthlyPrice> existingMonthlyMap = existingMonthly.stream()
                    .collect(Collectors.toMap(
                            p -> p.getStockCode() + "_" + p.getStartDate() + "_" + p.getEndDate(),
                            p -> p, (p1, p2) -> p1
                    ));

            List<StockMonthlyPrice> monthlyToSave = new ArrayList<>();
            for (StockMonthlyPrice newPrice : calculatedMonthlyPrices) {
                String key = newPrice.getStockCode() + "_" + newPrice.getStartDate() + "_" + newPrice.getEndDate();
                if (existingMonthlyMap.containsKey(key)) {
                    StockMonthlyPrice existing = existingMonthlyMap.get(key);
                    updateMonthlyPrice(existing, newPrice);
                    monthlyToSave.add(existing);
                } else {
                    monthlyToSave.add(newPrice);
                }
            }
            stockMonthlyPriceRepository.saveAll(monthlyToSave);
        }

        log.info("[Recovery] Weekly and monthly price re-calculation and upsert complete.");
    }

    private void updateStockPrice(StockPrice existing, StockPrice newPrice) {
        existing.setMarketCode(newPrice.getMarketCode());
        existing.setVolume(newPrice.getVolume());
        existing.setVolumePrice(newPrice.getVolumePrice());
        existing.setStartPrice(newPrice.getStartPrice());
        existing.setEndPrice(newPrice.getEndPrice());
        existing.setHighPrice(newPrice.getHighPrice());
        existing.setLowPrice(newPrice.getLowPrice());
        existing.setDailyRange(newPrice.getDailyRange());
        existing.setDailyRatio(newPrice.getDailyRatio());
        existing.setStockTotalCnt(newPrice.getStockTotalCnt());
        existing.setMarketTotalAmt(newPrice.getMarketTotalAmt());
    }

    private void updateWeeklyPrice(StockWeeklyPrice existing, StockWeeklyPrice newPrice) {
        existing.setMarketCode(newPrice.getMarketCode());
        existing.setVolume(newPrice.getVolume());
        existing.setVolumePrice(newPrice.getVolumePrice());
        existing.setStartPrice(newPrice.getStartPrice());
        existing.setEndPrice(newPrice.getEndPrice());
        existing.setHighPrice(newPrice.getHighPrice());
        existing.setLowPrice(newPrice.getLowPrice());
        existing.setStockTotalCnt(newPrice.getStockTotalCnt());
        existing.setMarketTotalAmt(newPrice.getMarketTotalAmt());
    }

    private void updateMonthlyPrice(StockMonthlyPrice existing, StockMonthlyPrice newPrice) {
        existing.setMarketCode(newPrice.getMarketCode());
        existing.setVolume(newPrice.getVolume());
        existing.setVolumePrice(newPrice.getVolumePrice());
        existing.setStartPrice(newPrice.getStartPrice());
        existing.setEndPrice(newPrice.getEndPrice());
        existing.setHighPrice(newPrice.getHighPrice());
        existing.setLowPrice(newPrice.getLowPrice());
        existing.setStockTotalCnt(newPrice.getStockTotalCnt());
        existing.setMarketTotalAmt(newPrice.getMarketTotalAmt());
    }
}
