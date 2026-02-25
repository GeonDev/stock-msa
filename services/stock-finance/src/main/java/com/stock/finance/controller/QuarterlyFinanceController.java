package com.stock.finance.controller;

import com.stock.common.enums.ReportCode;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.repository.CorpFinanceRepository;
import com.stock.finance.service.QuarterlyFinanceService;
import com.stock.finance.service.CorpFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/quarterly")
@RequiredArgsConstructor
@Tag(name = "Quarterly Finance", description = "분기별 재무 정보 API")
public class QuarterlyFinanceController {
    
    private final CorpFinanceRepository corpFinanceRepository;
    private final QuarterlyFinanceService quarterlyFinanceService;
    private final CorpFinanceService corpFinanceService;
    
    @GetMapping("/{corpCode}/{year}")
    @Operation(summary = "연도별 전체 분기 조회")
    public ResponseEntity<List<CorpFinance>> getYearlyQuarters(
            @Parameter(description = "기업 코드") @PathVariable String corpCode,
            @Parameter(description = "사업연도") @PathVariable String year) {
        
        List<CorpFinance> quarters = corpFinanceRepository.findByCorpCodeAndBizYearOrderByReportCode(corpCode, year);
        return ResponseEntity.ok(quarters);
    }
    
    @GetMapping("/{corpCode}/{year}/{reportCode}")
    @Operation(summary = "특정 분기 조회")
    public ResponseEntity<CorpFinance> getQuarter(
            @Parameter(description = "기업 코드") @PathVariable String corpCode,
            @Parameter(description = "사업연도") @PathVariable String year,
            @Parameter(description = "보고서 코드 (Q1, SEMI, Q3, ANNUAL)") @PathVariable String reportCode) {
        
        ReportCode code = ReportCode.valueOf(reportCode.toUpperCase());
        return corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(corpCode, year, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{corpCode}/{year}/{reportCode}/standalone")
    @Operation(summary = "분기 단독 실적 조회")
    public ResponseEntity<Map<String, BigDecimal>> getQuarterlyStandalone(
            @Parameter(description = "기업 코드") @PathVariable String corpCode,
            @Parameter(description = "사업연도") @PathVariable String year,
            @Parameter(description = "보고서 코드 (Q1, SEMI, Q3, ANNUAL)") @PathVariable String reportCode) {
        
        ReportCode code = ReportCode.valueOf(reportCode.toUpperCase());
        return corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(corpCode, year, code)
                .map(quarterlyFinanceService::calculateQuarterlyStandalone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{corpCode}/{year}/{reportCode}/qoq")
    @Operation(summary = "QoQ 성장률 조회")
    public ResponseEntity<Map<String, BigDecimal>> getQoQGrowth(
            @Parameter(description = "기업 코드") @PathVariable String corpCode,
            @Parameter(description = "사업연도") @PathVariable String year,
            @Parameter(description = "보고서 코드 (Q1, SEMI, Q3, ANNUAL)") @PathVariable String reportCode) {
        
        ReportCode code = ReportCode.valueOf(reportCode.toUpperCase());
        return corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(corpCode, year, code)
                .map(quarterlyFinanceService::calculateQoQGrowth)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{corpCode}/{year}/{reportCode}/yoy")
    @Operation(summary = "YoY 성장률 조회")
    public ResponseEntity<Map<String, BigDecimal>> getYoYGrowth(
            @Parameter(description = "기업 코드") @PathVariable String corpCode,
            @Parameter(description = "사업연도") @PathVariable String year,
            @Parameter(description = "보고서 코드 (Q1, SEMI, Q3, ANNUAL)") @PathVariable String reportCode) {
        
        ReportCode code = ReportCode.valueOf(reportCode.toUpperCase());
        return corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(corpCode, year, code)
                .map(quarterlyFinanceService::calculateYoYGrowth)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/verification")
    @Operation(summary = "재무 데이터 검증 통과율 조회")
    public ResponseEntity<Double> getVerificationRate() {
        return ResponseEntity.ok(corpFinanceService.getVerificationRate());
    }
}
