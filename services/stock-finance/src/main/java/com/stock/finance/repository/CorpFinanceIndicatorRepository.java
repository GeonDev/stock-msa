package com.stock.finance.repository;

import com.stock.finance.entity.CorpFinanceId;
import com.stock.finance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {

    List<CorpFinanceIndicator> findByCorpCodeInAndBasDt(List<String> corpCodes, java.time.LocalDate basDt);

}


