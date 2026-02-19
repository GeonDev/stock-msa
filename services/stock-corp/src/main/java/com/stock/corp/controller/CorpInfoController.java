package com.stock.corp.controller;


import com.stock.common.service.DayOffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class CorpInfoController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final DayOffService dayOffService;


    @PostMapping("/corp-info")
    @Operation(summary = "기업 기본 정보 수집", description = "상장 기업의 기본 마스터 정보를 수집합니다.")
    public ResponseEntity<String> corpInfoApi(
            @Parameter(description = "기준 일자 (yyyyMMdd, 미입력 시 전일)") 
            @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
            @RequestParam(value = "date", required = false) String date) throws Exception {

        if (!StringUtils.hasText(date)) {
            //금융위원회 데이터는 당일 데이터 조회 불가
            date = toLocalDateString(LocalDate.now().minusDays(1));
        }

        //공휴일 제외 값, 주말 제외
        if (dayOffService.checkIsDayOff(toStringLocalDate(date))) {
            return ResponseEntity.status(202).body("SKIPPED: " + date + " is a holiday or weekend");
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", date)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("corpDataJob"), jobParameters);

        return ResponseEntity.ok("BATCH STARTED: Corp info collection for " + date);
    }

    @PostMapping("/corp-detail/cleanup")
    @Operation(summary = "기업 상태 정리", description = "최신 정보로 갱신되지 않은 기업의 상태를 DEL로 변경합니다.")
    public ResponseEntity<String> cleanupCorpDetail() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("corpDetailCleanupJob"), jobParameters);

        return ResponseEntity.ok("CLEANUP STARTED");
    }

    @PostMapping("/corp-detail/sector-update")
    @Operation(summary = "업종 정보 수집", description = "OpenDART API를 통해 기업의 업종 정보를 수집 및 갱신합니다.")
    public ResponseEntity<String> updateSectorInfo() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("sectorUpdateJob"), jobParameters);

        return ResponseEntity.ok("SECTOR UPDATE STARTED");
    }
}
