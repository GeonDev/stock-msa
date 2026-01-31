package com.stock.batch.finance.repository;

import com.stock.batch.finance.entity.CorpFinanceId;
import com.stock.batch.finance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {
}
