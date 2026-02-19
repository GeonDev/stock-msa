package com.stock.finance.service;

import com.stock.common.enums.ReportCode;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.repository.CorpFinanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuarterlyFinanceService {
    
    private final CorpFinanceRepository corpFinanceRepository;
    
    /**
     * 분기 단독 실적 계산 (누적 데이터에서 이전 분기 차감)
     */
    public Map<String, BigDecimal> calculateQuarterlyStandalone(CorpFinance current) {
        Map<String, BigDecimal> result = new HashMap<>();
        
        ReportCode reportCode = current.getReportCode();
        
        // 1분기는 누적 = 단독
        if (reportCode == ReportCode.Q1) {
            result.put("revenue", current.getRevenue());
            result.put("opIncome", current.getOpIncome());
            result.put("netIncome", current.getNetIncome());
            result.put("operatingCashflow", current.getOperatingCashflow());
            return result;
        }
        
        // 이전 분기 조회
        ReportCode prevReportCode = reportCode.getPrevious();
        if (prevReportCode == null) {
            return result;
        }
        
        Optional<CorpFinance> prevOpt = corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(
                current.getCorpCode(), current.getBizYear(), prevReportCode);
        
        if (prevOpt.isEmpty()) {
            log.warn("Previous quarter not found: {} - {} - {}", 
                    current.getCorpCode(), current.getBizYear(), prevReportCode);
            return result;
        }
        
        CorpFinance prev = prevOpt.get();
        
        // 단독 실적 = 현재 누적 - 이전 누적
        result.put("revenue", subtract(current.getRevenue(), prev.getRevenue()));
        result.put("opIncome", subtract(current.getOpIncome(), prev.getOpIncome()));
        result.put("netIncome", subtract(current.getNetIncome(), prev.getNetIncome()));
        result.put("operatingCashflow", subtract(current.getOperatingCashflow(), prev.getOperatingCashflow()));
        
        return result;
    }
    
    /**
     * QoQ (Quarter over Quarter) 성장률 계산
     */
    public Map<String, BigDecimal> calculateQoQGrowth(CorpFinance current) {
        Map<String, BigDecimal> result = new HashMap<>();
        
        // 현재 분기 단독 실적
        Map<String, BigDecimal> currentQ = calculateQuarterlyStandalone(current);
        
        // 이전 분기 조회
        ReportCode prevReportCode = current.getReportCode().getPrevious();
        if (prevReportCode == null) {
            return result;  // 1분기는 이전 분기 없음
        }
        
        Optional<CorpFinance> prevOpt = corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(
                current.getCorpCode(), current.getBizYear(), prevReportCode);
        
        if (prevOpt.isEmpty()) {
            return result;
        }
        
        Map<String, BigDecimal> prevQ = calculateQuarterlyStandalone(prevOpt.get());
        
        // 성장률 계산
        result.put("qoqRevenueGrowth", calculateGrowthRate(currentQ.get("revenue"), prevQ.get("revenue")));
        result.put("qoqOpIncomeGrowth", calculateGrowthRate(currentQ.get("opIncome"), prevQ.get("opIncome")));
        result.put("qoqNetIncomeGrowth", calculateGrowthRate(currentQ.get("netIncome"), prevQ.get("netIncome")));
        
        return result;
    }
    
    /**
     * YoY (Year over Year) 성장률 계산
     */
    public Map<String, BigDecimal> calculateYoYGrowth(CorpFinance current) {
        Map<String, BigDecimal> result = new HashMap<>();
        
        // 현재 분기 단독 실적
        Map<String, BigDecimal> currentQ = calculateQuarterlyStandalone(current);
        
        // 전년 동기 조회
        int prevYear = Integer.parseInt(current.getBizYear()) - 1;
        Optional<CorpFinance> prevYearOpt = corpFinanceRepository.findByCorpCodeAndBizYearAndReportCode(
                current.getCorpCode(), String.valueOf(prevYear), current.getReportCode());
        
        if (prevYearOpt.isEmpty()) {
            return result;
        }
        
        Map<String, BigDecimal> prevYearQ = calculateQuarterlyStandalone(prevYearOpt.get());
        
        // 성장률 계산
        result.put("yoyRevenueGrowth", calculateGrowthRate(currentQ.get("revenue"), prevYearQ.get("revenue")));
        result.put("yoyOpIncomeGrowth", calculateGrowthRate(currentQ.get("opIncome"), prevYearQ.get("opIncome")));
        result.put("yoyNetIncomeGrowth", calculateGrowthRate(currentQ.get("netIncome"), prevYearQ.get("netIncome")));
        
        return result;
    }
    
    /**
     * BigDecimal 안전 차감
     */
    private BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return null;
        }
        return a.subtract(b);
    }
    
    /**
     * 성장률 계산 (%)
     */
    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
