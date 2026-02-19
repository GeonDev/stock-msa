package com.stock.finance.repository;

import com.stock.common.enums.ReportCode;
import com.stock.finance.entity.CorpFinance;
import com.stock.finance.entity.CorpFinanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorpFinanceRepository extends JpaRepository<CorpFinance, CorpFinanceId> {
    Optional<CorpFinance> findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(String corpCode, LocalDate basDt);
    
    List<CorpFinance> findByValidationStatusIsNull();
    
    // 분기별 조회
    Optional<CorpFinance> findByCorpCodeAndBizYearAndReportCode(String corpCode, String bizYear, ReportCode reportCode);
    
    // 이전 분기 조회 (QoQ 계산용)
    Optional<CorpFinance> findTop1ByCorpCodeAndBizYearAndReportCodeLessThanOrderByReportCodeDesc(
            String corpCode, String bizYear, ReportCode reportCode);
    
    // 특정 연도의 모든 분기 데이터 조회
    List<CorpFinance> findByCorpCodeAndBizYearOrderByReportCode(String corpCode, String bizYear);
}
