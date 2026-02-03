package com.stock.finance.controller;


import com.stock.common.service.DayOffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import static com.stock.common.utils.DateUtils.toLocalDateString;
import static com.stock.common.utils.DateUtils.toStringLocalDate;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
@Tag(name = "Batch API", description = "주식 및 기업 정보 수집을 위한 배치 실행 API")
public class CorpFinanceController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    private final DayOffService dayOffService;


    @PostMapping("/corp-fin")
    @Operation(summary = "기업 재무 정보 수집", description = "기업의 재무제표(매출, 영업이익 등) 데이터를 수집합니다.")
    public ResponseEntity<String> corpFinanceApi(
            @Parameter(description = "기준 일자 (yyyyMMdd, 미입력 시 전일)") @RequestParam(value = "date", required = false) String date) throws Exception {
        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값, 주말 제외
        if (!dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("corpFinanceJob"), jobParameters);
        }

        return ResponseEntity.ok("SET CORP FINANCE");
    }


    @PostMapping("/corp-fin/recovery")
    @Operation(summary = "기업 재무 정보 재수집", description = "특정 기간의 재무 데이터를 재수집 합니다.")
    public ResponseEntity<String> recoveryCorpFinanceApi(
            @Parameter(description = "시작 연도 (yyyy)", example = "2020") @RequestParam(value = "startYear") int startYear,
            @Parameter(description = "종료 연도 (yyyy)", example = "2025") @RequestParam(value = "endYear") int endYear) throws Exception {

        for (int year = startYear; year <= endYear; year++) {
            // 각 연도별로 배치 실행 (날짜는 해당 연도의 1월 1일로 설정하여 Reader에서 연도 추출)
            String dateParam = year + "0101";
            
            log.info("Starting recovery batch for year: {}", year);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", dateParam)
                    .addLong("time", System.currentTimeMillis())
                    .addString("recovery", "true") // 복구 작업임을 표시
                    .toJobParameters();

            jobLauncher.run(jobRegistry.getJob("corpFinanceJob"), jobParameters);
        }

        return ResponseEntity.ok("RECOVERY STARTED for " + startYear + " - " + endYear);
    }
}
