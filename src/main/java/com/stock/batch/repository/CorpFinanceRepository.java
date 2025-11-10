package com.stock.batch.repository;

import com.stock.batch.entity.CorpFinance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpFinanceRepository extends JpaRepository<CorpFinance, String> {
}
