package com.stock.finance.repository;

import com.stock.finance.entity.CorpFinanceId;
import com.stock.finance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {
}
