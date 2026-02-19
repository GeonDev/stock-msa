package com.stock.finance.service;

import com.stock.common.enums.ReportCode;
import com.stock.finance.dto.DartAccount;
import com.stock.finance.entity.CorpFinance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class DartFinanceConverter {
    
    /**
     * DART 계정과목 리스트를 CorpFinance 엔티티로 변환
     */
    public CorpFinance convertToCorpFinance(List<DartAccount> accounts, String corpCode, String year, ReportCode reportCode) {
        if (accounts == null || accounts.isEmpty()) {
            return null;
        }
        
        CorpFinance finance = new CorpFinance();
        finance.setCorpCode(corpCode);
        finance.setBizYear(year);
        finance.setReportCode(reportCode);
        
        // bas_dt 설정 (보고서 코드에 따라 자동 계산)
        LocalDate basDt = reportCode.getBasDt(Integer.parseInt(year));
        finance.setBasDt(basDt);
        
        for (DartAccount account : accounts) {
            String accountNm = account.getAccountNm();
            if (accountNm == null) {
                continue;
            }
            
            BigDecimal amount = parseBigDecimal(account.getThstrmAmount());
            
            // 재무상태표 (BS)
            if ("BS".equals(account.getSjDiv())) {
                if (accountNm.contains("자산총계") || accountNm.equals("자산총계")) {
                    finance.setTotalAsset(amount);
                } else if (accountNm.contains("부채총계") || accountNm.equals("부채총계")) {
                    finance.setTotalDebt(amount);
                } else if (accountNm.contains("자본총계") || accountNm.equals("자본총계")) {
                    finance.setTotalCapital(amount);
                }
            }
            
            // 손익계산서 (IS)
            if ("IS".equals(account.getSjDiv())) {
                if (accountNm.contains("매출액") || accountNm.contains("수익(매출액)")) {
                    if (finance.getRevenue() == null) {  // 첫 번째 매출액만 사용
                        finance.setRevenue(amount);
                    }
                } else if (accountNm.contains("영업이익") && !accountNm.contains("금융")) {
                    if (finance.getOpIncome() == null) {
                        finance.setOpIncome(amount);
                    }
                } else if (accountNm.contains("당기순이익") || accountNm.equals("당기순이익")) {
                    if (finance.getNetIncome() == null) {
                        finance.setNetIncome(amount);
                    }
                } else if (accountNm.contains("감가상각비")) {
                    if (finance.getDepreciation() == null) {
                        finance.setDepreciation(amount);
                    }
                }
            }
            
            // 현금흐름표 (CF)
            if ("CF".equals(account.getSjDiv())) {
                if (accountNm.contains("영업활동") && accountNm.contains("현금흐름")) {
                    if (finance.getOperatingCashflow() == null) {
                        finance.setOperatingCashflow(amount);
                    }
                } else if (accountNm.contains("투자활동") && accountNm.contains("현금흐름")) {
                    if (finance.getInvestingCashflow() == null) {
                        finance.setInvestingCashflow(amount);
                    }
                } else if (accountNm.contains("재무활동") && accountNm.contains("현금흐름")) {
                    if (finance.getFinancingCashflow() == null) {
                        finance.setFinancingCashflow(amount);
                    }
                }
            }
        }
        
        // FCF 계산 (영업CF - 투자CF)
        if (finance.getOperatingCashflow() != null && finance.getInvestingCashflow() != null) {
            finance.setFreeCashflow(
                finance.getOperatingCashflow().add(finance.getInvestingCashflow())
            );
        }
        
        // EBITDA 계산 (영업이익 + 감가상각비)
        if (finance.getOpIncome() != null && finance.getDepreciation() != null) {
            finance.setEbitda(
                finance.getOpIncome().add(finance.getDepreciation())
            );
        }
        
        return finance;
    }
    
    /**
     * 문자열을 BigDecimal로 변환
     * DART API는 금액을 문자열로 반환하며, 쉼표가 포함될 수 있음
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return null;
        }
        
        try {
            // 쉼표 제거 및 공백 제거
            String cleaned = value.replace(",", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal: {}", value);
            return null;
        }
    }
}
