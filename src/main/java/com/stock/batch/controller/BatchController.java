package com.stock.batch.controller;


import com.stock.batch.enums.StockMarket;
import com.stock.batch.service.DayOffService;
import com.stock.batch.service.StockApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.stock.batch.utils.DateUtils.toLocalDateString;
import static com.stock.batch.utils.DateUtils.toStringLocalDate;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    private final DayOffService dayOffService;

    @PostMapping("/price")
    public ResponseEntity<String> stockPriceApi(@RequestParam(value = "market") StockMarket marketType, @RequestParam(value = "date", required = false) String date) throws Exception {

        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값
        if (dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addString("market", marketType.name())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("stockDataJob"), jobParameters);
        }

        return ResponseEntity.ok("SET PRICE");
    }

    @PostMapping("/corp-info")
    public ResponseEntity<String> corpInfoApi( @RequestParam(value = "date", required = false) String date) throws Exception {

        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값,
        if (dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("corpDataJob"), jobParameters);
        }


        return ResponseEntity.ok("SET CORP CODE");
    }

    @PostMapping("/corp-fin")
    public ResponseEntity<String> corpFinApi( @RequestParam(value = "date", required = false) String date) throws Exception {
        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값,
        if (dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("corpFinanceJob"), jobParameters);
        }



        return ResponseEntity.ok("SET CORP FINANCE");
    }




}
