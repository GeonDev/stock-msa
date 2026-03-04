package com.stock.finance.repository;

import com.stock.finance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, Long> {

    List<CorpFinanceIndicator> findByCorpCodeInAndBasDt(List<String> corpCodes, LocalDate basDt);

    @Query(value = "SELECT * FROM TB_CORP_FINANCE_INDICATOR i " +
                   "WHERE i.corp_code IN (:corpCodes) " +
                   "AND i.bas_dt = (" +
                   "  SELECT MAX(bas_dt) FROM TB_CORP_FINANCE_INDICATOR i2 " +
                   "  WHERE i2.corp_code = i.corp_code " +
                   "  AND i2.bas_dt <= :basDt" +
                   ")", nativeQuery = true)
    List<CorpFinanceIndicator> findLatestByCorpCodeInAndBasDtBefore(
            @Param("corpCodes") List<String> corpCodes, 
            @Param("basDt") LocalDate basDt);

    Optional<CorpFinanceIndicator> findTopByCorpCodeOrderByBasDtDesc(String corpCode);
}


