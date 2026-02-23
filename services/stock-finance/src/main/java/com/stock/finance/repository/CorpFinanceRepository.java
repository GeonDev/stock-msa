package com.stock.finance.repository;

import com.stock.common.enums.ReportCode;
import com.stock.finance.entity.CorpFinance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorpFinanceRepository extends JpaRepository<CorpFinance, Long> {
    Optional<CorpFinance> findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(String corpCode, LocalDate basDt);

    List<CorpFinance> findByValidationStatusIsNull();

    Optional<CorpFinance> findByCorpCodeAndBizYearAndReportCode(String corpCode, String bizYear, ReportCode reportCode);

    Optional<CorpFinance> findTop1ByCorpCodeAndBizYearAndReportCodeLessThanOrderByReportCodeDesc(
            String corpCode, String bizYear, ReportCode reportCode);

    List<CorpFinance> findByCorpCodeAndBizYearOrderByReportCode(String corpCode, String bizYear);
}
