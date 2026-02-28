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
    @Operation(summary = "기업 재무 정보 수집", description = "기업의 재무제표 데이터를 수집합니다. reportCode 미입력 시 4개 분기 전체 수집.")
    public ResponseEntity<String> corpFinanceApi(
            @Parameter(description = "기준 일자 (yyyyMMdd, 미입력 시 전일)")
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "date", required = false) String date,
            @Parameter(description = "분기 코드 (Q1, SEMI, Q3, ANNUAL), 미입력 시 전체 수집")
            @RequestParam(value = "reportCode", required = false) String reportCode) throws Exception {

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

        jobLauncher.run(jobRegistry.getJob("corpFinanceJob"), builder.toJobParameters());

        String target = StringUtils.hasText(reportCode) ? reportCode.toUpperCase() : "ALL";
        return ResponseEntity.ok("BATCH STARTED: Corp finance [" + target + "] for " + date);
    }


    @PostMapping("/corp-fin/recovery")
    @Operation(summary = "기업 재무 정보 재수집", description = "특정 기간의 재무 데이터를 재수집 합니다.")
    public ResponseEntity<String> recoveryCorpFinanceApi(
            @Parameter(description = "시작 연도 (yyyy)", example = "2020") 
            @Min(value = 2000, message = "시작 연도는 2000년 이상이어야 합니다")
            @Max(value = 2100, message = "시작 연도는 2100년 이하여야 합니다")
            @RequestParam(value = "startYear") int startYear,
            @Parameter(description = "종료 연도 (yyyy)", example = "2025") 
            @Min(value = 2000, message = "종료 연도는 2000년 이상이어야 합니다")
            @Max(value = 2100, message = "종료 연도는 2100년 이하여야 합니다")
            @RequestParam(value = "endYear") int endYear) throws Exception {

        corpFinanceService.recoverCorpFinance(startYear, endYear);

        return ResponseEntity.ok("RECOVERY STARTED for " + startYear + " - " + endYear);
    }
}
