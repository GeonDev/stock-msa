package com.stock.finance.service;

import com.stock.common.enums.ValidationStatus;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.repository.CorpFinanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceValidationService {

    private final CorpFinanceRepository corpFinanceRepository;
    private static final BigDecimal BALANCE_TOLERANCE_RATE = BigDecimal.valueOf(0.001); // 0.1%

    @Transactional
    public void validateAndSave(CorpFinance finance) {
        ValidationStatus status = performValidation(finance);
        finance.setValidationStatus(status);
        corpFinanceRepository.save(finance);
    }

    /**
     * 재무 데이터 정합성 검증 수행
     */
    public ValidationStatus performValidation(CorpFinance finance) {
        // 1. 필수 필드 검사
        if (hasNullOrZeroRequiredFields(finance)) {
            log.warn("[Validation] Missing required fields for corp: {}, date: {}", finance.getCorpCode(), finance.getBasDt());
            return ValidationStatus.ERROR_MISSING;
        }

        // 2. 대차대조표 등식 검증 (자산 = 부채 + 자본)
        if (!isBalanceSheetValid(finance)) {
            log.warn("[Validation] Balance sheet mismatch for corp: {}, date: {}", finance.getCorpCode(), finance.getBasDt());
            return ValidationStatus.ERROR_IDENTITY;
        }

        // 3. 이상치 탐지
        if (isOutlierDetected(finance)) {
             log.info("[Validation] Outlier detected for corp: {}, date: {}", finance.getCorpCode(), finance.getBasDt());
             return ValidationStatus.WARN_OUTLIER;
        }

        return ValidationStatus.VERIFIED;
    }

    private boolean isOutlierDetected(CorpFinance current) {
        return corpFinanceRepository.findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(current.getCorpCode(), current.getBasDt())
                .map(prev -> {
                    // 자산 변동폭 체크 (1000% 이상)
                    if (isChangeRateExceeded(prev.getTotalAsset(), current.getTotalAsset(), 10.0)) return true;
                    // 매출 변동폭 체크 (1000% 이상)
                    if (isChangeRateExceeded(prev.getRevenue(), current.getRevenue(), 10.0)) return true;
                    return false;
                })
                .orElse(false); // 이전 데이터 없으면 이상치 판단 불가 (Pass)
    }

    private boolean isChangeRateExceeded(BigDecimal prevVal, BigDecimal currVal, double thresholdRate) {
        if (prevVal == null || currVal == null || prevVal.compareTo(BigDecimal.ZERO) == 0) return false;
        
        BigDecimal diff = currVal.subtract(prevVal).abs();
        BigDecimal rate = diff.divide(prevVal.abs(), MathContext.DECIMAL64); // 정밀도 확보
        
        return rate.compareTo(BigDecimal.valueOf(thresholdRate)) > 0;
    }

    private boolean hasNullOrZeroRequiredFields(CorpFinance f) {
        return isNullOrZero(f.getTotalAsset()) ||
               isNullOrZero(f.getTotalDebt()) ||
               isNullOrZero(f.getTotalCapital()) ||
               isNullOrZero(f.getRevenue());
    }

    private boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isBalanceSheetValid(CorpFinance f) {
        BigDecimal assets = f.getTotalAsset() != null ? f.getTotalAsset() : BigDecimal.ZERO;
        BigDecimal debt = f.getTotalDebt() != null ? f.getTotalDebt() : BigDecimal.ZERO;
        BigDecimal capital = f.getTotalCapital() != null ? f.getTotalCapital() : BigDecimal.ZERO;

        BigDecimal sum = debt.add(capital);
        BigDecimal diff = assets.subtract(sum).abs();

        // 자산이 0이면 위에서 걸러지겠지만, 안전장치
        if (assets.compareTo(BigDecimal.ZERO) == 0) return false;

        // 오차율 계산: diff / assets
        BigDecimal errorRate = diff.divide(assets, MathContext.DECIMAL64);
        return errorRate.compareTo(BALANCE_TOLERANCE_RATE) <= 0;
    }
}
