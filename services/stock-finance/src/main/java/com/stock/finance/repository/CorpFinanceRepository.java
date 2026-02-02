package com.stock.finance.repository;

import com.stock.finance.entity.CorpFinance;
import com.stock.finance.entity.CorpFinanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CorpFinanceRepository extends JpaRepository<CorpFinance, CorpFinanceId> {
    Optional<CorpFinance> findTop1ByCorpCodeAndBasDtBeforeOrderByBasDtDesc(String corpCode, LocalDate basDt);
}
