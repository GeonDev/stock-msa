package com.stock.price.controller;


import com.stock.price.service.RecoveryService;
import com.stock.common.enums.StockMarket;
import com.stock.common.service.DayOffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.stock.common.utils.DateUtils.toLocalDateString;
import static com.stock.common.utils.DateUtils.toStringLocalDate;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
@Tag(name = "Batch API", description = "주식 및 기업 정보 수집을 위한 배치 실행 API")
public class StockController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final RecoveryService recoveryService;

    private final DayOffService dayOffService;

    @PostMapping("/price")
    @Operation(summary = "주식 가격 정보 수집", description = "지정된 시장과 날짜의 주식 시세 데이터를 수집합니다.")
    public ResponseEntity<String> stockPriceApi(
            @Parameter(description = "시장 구분 (KOSPI, KOSDAQ 등)") 
            @NotNull(message = "시장 구분은 필수입니다") 
            @RequestParam(value = "market") StockMarket marketType,
            @Parameter(description = "기준 일자 (yyyyMMdd, 미입력 시 전일)") 
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "date", required = false) String date) throws Exception {

        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값, 주말 제외
        if (!dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addString("market", marketType.name())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("stockDataJob"), jobParameters);
        }

        return ResponseEntity.ok("SET PRICE");
    }


    @PostMapping("/price-weekly")
    @Operation(summary = "주간 주식 가격 집계", description = "수집된 일별 데이터를 기반으로 주간 단위 시세를 집계합니다.")
    public ResponseEntity<String> weeklyStockPrice() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("weeklyStockDataJob"), jobParameters);

        return ResponseEntity.ok("SET WEEKLY PRICE");
    }

    @PostMapping("/price-monthly")
    @Operation(summary = "월간 주식 가격 집계", description = "수집된 일별 데이터를 기반으로 월간 단위 시세를 집계합니다.")
    public ResponseEntity<String> monthlyStockPrice() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("monthlyStockDataJob"), jobParameters);

        return ResponseEntity.ok("SET MONTHLY PRICE");
    }

    @PostMapping("/price/recovery")
    @Operation(summary = "주식 시세 정보 재수집", description = "특정 기간의 일별, 주별, 월별 시세 데이터를 재수집합니다.")
    public ResponseEntity<String> recoveryStockPriceApi(
            @Parameter(description = "시작일 (yyyyMMdd)", example = "20240101") 
            @NotNull(message = "시작일은 필수입니다")
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "startDate") String startDate,
            @Parameter(description = "종료일 (yyyyMMdd)", example = "20240331") 
            @NotNull(message = "종료일은 필수입니다")
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "endDate") String endDate) {

        recoveryService.recoverStockPrices(toStringLocalDate(startDate), toStringLocalDate(endDate));

        return ResponseEntity.ok("Price recovery process started for " + startDate + " - " + endDate);
    }
}
