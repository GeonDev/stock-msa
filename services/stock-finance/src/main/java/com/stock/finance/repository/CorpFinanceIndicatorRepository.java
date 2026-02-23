package com.stock.finance.repository;

import com.stock.finance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, Long> {

    List<CorpFinanceIndicator> findByCorpCodeInAndBasDt(List<String> corpCodes, LocalDate basDt);

    Optional<CorpFinanceIndicator> findTopByCorpCodeOrderByBasDtDesc(String corpCode);
}


