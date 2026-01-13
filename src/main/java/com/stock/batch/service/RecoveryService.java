package com.stock.batch.service;

import com.stock.batch.entity.StockMonthlyPrice;
import com.stock.batch.entity.StockPrice;
import com.stock.batch.entity.StockWeeklyPrice;
import com.stock.batch.enums.StockMarket;
import com.stock.batch.repository.StockMonthlyPriceRepository;
import com.stock.batch.repository.StockPriceRepository;
import com.stock.batch.repository.StockWeeklyPriceRepository;
import com.stock.batch.utils.DateUtils;
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
    private final StockApiService stockApiService;
    private final PriceCalculationService priceCalculationService;
    private final DayOffService dayOffService;

    @Transactional
    public void recoverStockPrices(LocalDate startDate, LocalDate endDate) {
        // Phase 1: Re-populate Daily Prices (Memory-Efficient)
        log.info("[Recovery] Deleting existing daily price data from {} to {}", startDate, endDate);
        stockPriceRepository.deleteByBasDtBetween(startDate, endDate);
        log.info("[Recovery] Deletion complete.");

        log.info("[Recovery] Re-fetching daily prices from {} to {}", startDate, endDate);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (dayOffService.checkIsDayOff(date)) {
                log.info("[Recovery] Skipping holiday: {}", date);
                continue;
            }
            try {
                String dateStr = DateUtils.toLocalDateString(date);
                log.info("[Recovery] Fetching and saving daily prices for {}", dateStr);
                List<StockPrice> kospiPrices = stockApiService.getStockPrice(StockMarket.KOSPI, dateStr);
                List<StockPrice> kosdaqPrices = stockApiService.getStockPrice(StockMarket.KOSDAQ, dateStr);

                List<StockPrice> dailyPrices = new ArrayList<>();
                dailyPrices.addAll(kospiPrices);
                dailyPrices.addAll(kosdaqPrices);

                if (!dailyPrices.isEmpty()) {
                    stockPriceRepository.saveAll(dailyPrices);
                }
            } catch (Exception e) {
                log.error("[Recovery] Failed to fetch price data for date: {}", date, e);
            }
        }
        log.info("[Recovery] Daily price re-fetching and saving complete.");

        // Phase 2: Recalculate Aggregates (Correctly and Efficiently)
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

        // Delete old aggregates for affected stocks in the wider range
        log.info("[Recovery] Deleting old weekly aggregates from {} to {} for {} stocks.", recalcWeeklyStart, recalcWeeklyEnd, affectedStockCodes.size());
        stockWeeklyPriceRepository.deleteByStockCodeInAndEndDateBetween(affectedStockCodes, recalcWeeklyStart, recalcWeeklyEnd);
        log.info("[Recovery] Deleting old monthly aggregates from {} to {} for {} stocks.", recalcMonthlyStart, recalcMonthlyEnd, affectedStockCodes.size());
        stockMonthlyPriceRepository.deleteByStockCodeInAndEndDateBetween(affectedStockCodes, recalcMonthlyStart, recalcMonthlyEnd);

        // Fetch all necessary daily prices from DB for the recalculation
        LocalDate overallRecalcStart = recalcWeeklyStart.isBefore(recalcMonthlyStart) ? recalcWeeklyStart : recalcMonthlyStart;
        LocalDate overallRecalcEnd = recalcWeeklyEnd.isAfter(recalcMonthlyEnd) ? recalcWeeklyEnd : recalcMonthlyEnd;
        log.info("[Recovery] Fetching daily prices from DB for recalculation from {} to {}.", overallRecalcStart, overallRecalcEnd);
        List<StockPrice> pricesForRecalc = stockPriceRepository.findByStockCodeInAndBasDtBetween(affectedStockCodes, overallRecalcStart, overallRecalcEnd);

        // Group prices by stock for efficient calculation
        Map<String, List<StockPrice>> pricesByStock = pricesForRecalc.stream()
                .collect(Collectors.groupingBy(StockPrice::getStockCode));

        List<StockWeeklyPrice> allWeeklyPrices = new ArrayList<>();
        List<StockMonthlyPrice> allMonthlyPrices = new ArrayList<>();

        // Recalculate and collect new aggregates
        for (List<StockPrice> dailyPrices : pricesByStock.values()) {
            allWeeklyPrices.addAll(priceCalculationService.calculateWeeklyPrices(dailyPrices));
            allMonthlyPrices.addAll(priceCalculationService.calculateMonthlyPrices(dailyPrices));
        }

        // Batch save the new aggregates
        if (!allWeeklyPrices.isEmpty()) {
            stockWeeklyPriceRepository.saveAll(allWeeklyPrices);
        }
        if (!allMonthlyPrices.isEmpty()) {
            stockMonthlyPriceRepository.saveAll(allMonthlyPrices);
        }
        log.info("[Recovery] Weekly and monthly price re-calculation complete. Saved {} weekly and {} monthly records.", allWeeklyPrices.size(), allMonthlyPrices.size());
    }
}
