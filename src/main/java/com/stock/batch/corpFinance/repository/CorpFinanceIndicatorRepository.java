package com.stock.batch.corpFinance.repository;

import com.stock.batch.corpFinance.entity.CorpFinanceId;
import com.stock.batch.corpFinance.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {
}
