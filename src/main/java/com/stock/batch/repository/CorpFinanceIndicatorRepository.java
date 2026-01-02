package com.stock.batch.repository;

import com.stock.batch.entity.CorpFinanceIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, String> {
}
