package com.stock.finance.service;

import com.stock.common.enums.ReportCode;
import com.stock.finance.client.CorpClient;
import com.stock.finance.client.DartClient;
import com.stock.finance.dto.DartFinancialResponse;
import com.stock.finance.entity.CorpFinanceIndicator;
import com.stock.finance.entity.CorpFinance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorpFinanceService {

    private final DartClient dartClient;
    private final DartFinanceConverter dartFinanceConverter;
    private final CorpClient corpClient;
    private final QuarterlyFinanceService quarterlyFinanceService;


    /**
     * 재무 정보 조회 (DART API)
     */
    public List<CorpFinance> getCorpFinance(String bizYear, ReportCode reportCode) throws Exception {
        return getCorpFinanceFromDart(bizYear, reportCode);
    }

    /**
     * DART API로 재무 정보 조회
     * @param reportCode null이면 4개 분기 전체 수집, 지정 시 해당 분기만 수집
     */
    private List<CorpFinance> getCorpFinanceFromDart(String bizYear, ReportCode reportCode) {
        List<CorpFinance> result = new ArrayList<>();

        List<String> stockCodes = corpClient.getAllStockCodes();
        if (stockCodes == null || stockCodes.isEmpty()) {
            log.warn("No stock codes found from corp service");
            return result;
        }

        ReportCode[] reportCodes = (reportCode != null)
                ? new ReportCode[]{reportCode}
                : new ReportCode[]{ReportCode.Q1, ReportCode.SEMI, ReportCode.Q3, ReportCode.ANNUAL};

        log.info("Fetching {} financial data for {} companies from DART",
                reportCode != null ? reportCode : "ALL quarters", stockCodes.size());

        int successCount = 0;
        int failCount = 0;

        for (String stockCode : stockCodes) {
            try {
                // DB에서 DART 고유번호 조회
                String corpCode = corpClient.getDartCorpCode(stockCode);
                if (corpCode == null || corpCode.isEmpty()) {
                    failCount++;
                    continue;
                }

                for (ReportCode rc : reportCodes) {
                    try {
                        DartFinancialResponse response = dartClient.getFinancialStatement(
                                corpCode, bizYear, rc.getCode(), "CFS");

                        if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                            CorpFinance finance = dartFinanceConverter.convertToCorpFinance(
                                    response.getList(), stockCode, bizYear, rc);
                            if (finance != null) {
                                result.add(finance);
                                successCount++;
                            }
                        }

                    } catch (Exception e) {
                        log.warn("Failed to fetch DART data: {} - {} - {}", stockCode, bizYear, rc);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to fetch DART data for stock: {}", stockCode, e);
                failCount++;
            }
        }

        log.info("DART API fetch completed: {} success, {} failed", successCount, failCount);
        return result;
    }
    

    public CorpFinanceIndicator calculateIndicators(CorpFinance currentFinance, BigDecimal marketCap) {
        CorpFinanceIndicator.CorpFinanceIndicatorBuilder builder = CorpFinanceIndicator.builder()
                .corpCode(currentFinance.getCorpCode())
                .basDt(currentFinance.getBasDt())
                .reportCode(currentFinance.getReportCode());

        // Calculate ROE, ROA, Debt Ratio from current data
        if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital().compareTo(BigDecimal.ZERO) != 0) {
            if (currentFinance.getNetIncome() != null) {
                builder.roe(currentFinance.getNetIncome()
                        .divide(currentFinance.getTotalCapital(), 8, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
        }
        if (currentFinance.getTotalAsset() != null && currentFinance.getTotalAsset().compareTo(BigDecimal.ZERO) != 0 && currentFinance.getNetIncome() != null) {
            builder.roa(currentFinance.getNetIncome()
                    .divide(currentFinance.getTotalAsset(), 8, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        // Calculate PER, PBR, PSR from current data and market cap
        if (marketCap != null && marketCap.compareTo(BigDecimal.ZERO) > 0) {
            if (currentFinance.getNetIncome() != null && currentFinance.getNetIncome().compareTo(BigDecimal.ZERO) > 0) {
                builder.per(marketCap.divide(currentFinance.getNetIncome(), 8, java.math.RoundingMode.HALF_UP));
            }
            if (currentFinance.getTotalCapital() != null && currentFinance.getTotalCapital().compareTo(BigDecimal.ZERO) > 0) {
                builder.pbr(marketCap.divide(currentFinance.getTotalCapital(), 8, java.math.RoundingMode.HALF_UP));
            }
            if (currentFinance.getRevenue() != null && currentFinance.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                builder.psr(marketCap.divide(currentFinance.getRevenue(), 8, java.math.RoundingMode.HALF_UP));
            }
            
            // PCR (Price to Cashflow Ratio)
            if (currentFinance.getOperatingCashflow() != null && currentFinance.getOperatingCashflow().compareTo(BigDecimal.ZERO) > 0) {
                builder.pcr(marketCap.divide(currentFinance.getOperatingCashflow(), 8, java.math.RoundingMode.HALF_UP));
            }
            
            // FCF Yield (%)
            if (currentFinance.getFreeCashflow() != null && currentFinance.getFreeCashflow().compareTo(BigDecimal.ZERO) > 0) {
                builder.fcfYield(currentFinance.getFreeCashflow()
                        .divide(marketCap, 8, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
            
            // EV/EBITDA (간단 계산: 시가총액 + 순부채 / EBITDA)
            if (currentFinance.getEbitda() != null && currentFinance.getEbitda().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal netDebt = BigDecimal.ZERO;
                if (currentFinance.getTotalDebt() != null) {
                    netDebt = currentFinance.getTotalDebt();
                }
                BigDecimal enterpriseValue = marketCap.add(netDebt);
                builder.evEbitda(enterpriseValue.divide(currentFinance.getEbitda(), 8, java.math.RoundingMode.HALF_UP));
            }
        }
        
        // Operating Margin (%)
        if (currentFinance.getRevenue() != null && currentFinance.getRevenue().compareTo(BigDecimal.ZERO) > 0 
                && currentFinance.getOpIncome() != null) {
            builder.operatingMargin(currentFinance.getOpIncome()
                    .divide(currentFinance.getRevenue(), 8, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        
        // Net Margin (%)
        if (currentFinance.getRevenue() != null && currentFinance.getRevenue().compareTo(BigDecimal.ZERO) > 0 
                && currentFinance.getNetIncome() != null) {
            builder.netMargin(currentFinance.getNetIncome()
                    .divide(currentFinance.getRevenue(), 8, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        // QoQ 성장률 계산
        Map<String, BigDecimal> qoqGrowth = quarterlyFinanceService.calculateQoQGrowth(currentFinance);
        builder.qoqRevenueGrowth(qoqGrowth.get("qoqRevenueGrowth"));
        builder.qoqOpIncomeGrowth(qoqGrowth.get("qoqOpIncomeGrowth"));
        builder.qoqNetIncomeGrowth(qoqGrowth.get("qoqNetIncomeGrowth"));
        
        // YoY 성장률 계산
        Map<String, BigDecimal> yoyGrowth = quarterlyFinanceService.calculateYoYGrowth(currentFinance);
        builder.yoyRevenueGrowth(yoyGrowth.get("yoyRevenueGrowth"));
        builder.yoyOpIncomeGrowth(yoyGrowth.get("yoyOpIncomeGrowth"));
        builder.yoyNetIncomeGrowth(yoyGrowth.get("yoyNetIncomeGrowth"));

        return builder.build();
    }
}