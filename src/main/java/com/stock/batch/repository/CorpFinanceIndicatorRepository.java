package com.stock.batch.repository;

import com.stock.batch.entity.CorpFinanceId;
import com.stock.batch.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {
}
