package com.stock.price.service;

import com.stock.common.enums.StockMarket;
import com.stock.common.service.DayOffService;
import com.stock.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final JobLauncher jobLauncher;
    private final Job stockPriceRecoveryJob;
    private final DayOffService dayOffService;

    public void recoverStockPrices(LocalDate startDate, LocalDate endDate) {
        log.info("[Recovery] Starting batch-based recovery from {} to {}", startDate, endDate);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (dayOffService.checkIsDayOff(date)) {
                log.info("[Recovery] Skipping holiday: {}", date);
                continue;
            }

            String dateStr = DateUtils.toLocalDateString(date);
            
            // KOSPI와 KOSDAQ 각각에 대해 복구 배치 실행
            runRecoveryJob(dateStr, StockMarket.KOSPI);
            runRecoveryJob(dateStr, StockMarket.KOSDAQ);
        }
        
        log.info("[Recovery] Batch-based recovery process initiated for all dates.");
    }

    private void runRecoveryJob(String date, StockMarket market) {
        try {
            log.info("[Recovery] Launching recovery job for {} - {}", date, market);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addString("market", market.name())
                    .addLong("time", System.currentTimeMillis())
                    .addString("recovery", "true")
                    .toJobParameters();

            jobLauncher.run(stockPriceRecoveryJob, jobParameters);
        } catch (Exception e) {
            log.error("[Recovery] Failed to launch recovery job for {} - {}: {}", date, market, e.getMessage());
        }
    }
}
