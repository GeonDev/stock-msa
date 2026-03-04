package com.stock.finance.controller;


import com.stock.common.service.DayOffService;
import com.stock.finance.service.CorpFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.stock.common.utils.DateUtils.toLocalDateString;
import static com.stock.common.utils.DateUtils.toStringLocalDate;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
@Tag(name = "Batch API", description = "주식 및 기업 정보 수집을 위한 배치 실행 API")
public class CorpFinanceController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final DayOffService dayOffService;
    private final CorpFinanceService corpFinanceService;


    @PostMapping("/corp-fin")
    @Operation(summary = "기업 재무 정보 수집", description = "기업의 재무제표 데이터를 수집합니다. corpCode 입력 시 해당 기업만, 미입력 시 전체 수집. reportCode 미입력 시 4개 분기 전체 수집.")
    public ResponseEntity<String> corpFinanceApi(
            @Parameter(description = "기준 일자 (yyyyMMdd, 미입력 시 전일)")
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "date", required = false) String date,
            @Parameter(description = "분기 코드 (Q1, SEMI, Q3, ANNUAL), 미입력 시 전체 수집")
            @RequestParam(value = "reportCode", required = false) String reportCode,
            @Parameter(description = "기업 코드 (8자리), 미입력 시 전체 기업 수집")
            @RequestParam(value = "corpCode", required = false) String corpCode) throws Exception {

        if (!StringUtils.hasText(date)) {
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        if (dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            return ResponseEntity.status(202).body("SKIPPED: " + date + " is a holiday or weekend");
        }

        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("date", date)
                .addLong("time", System.currentTimeMillis());

        if (StringUtils.hasText(reportCode)) {
            builder.addString("reportCode", reportCode.toUpperCase());
        }

        if (StringUtils.hasText(corpCode)) {
            builder.addString("corpCode", corpCode);
        }

        jobLauncher.run(jobRegistry.getJob("corpFinanceJob"), builder.toJobParameters());

        String targetReport = StringUtils.hasText(reportCode) ? reportCode.toUpperCase() : "ALL";
        String targetCorp = StringUtils.hasText(corpCode) ? corpCode : "ALL";
        return ResponseEntity.ok("BATCH STARTED: Corp finance [Corp: " + targetCorp + ", Report: " + targetReport + "] for " + date);
    }


    @PostMapping("/corp-fin/validate")
    @Operation(summary = "재무 데이터 검증 실행", description = "DB에 저장된 재무 데이터 중 검증되지 않은 데이터를 검증합니다.")
    public ResponseEntity<String> validateFinanceApi() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("validateFinanceJob"), jobParameters);

        return ResponseEntity.ok("VALIDATION STARTED");
    }

    @PostMapping("/corp-fin/recalculate-indicators")
    @Operation(summary = "재무 지표 재계산", description = "DB에 저장된 재무 데이터를 기반으로 지표를 다시 계산합니다.")
    public ResponseEntity<String> recalculateIndicatorsApi() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("recalculateIndicatorsJob"), jobParameters);

        return ResponseEntity.ok("RECALCULATION STARTED");
    }
}
