package com.stock.batch.repository;

import com.stock.batch.entity.StockIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {
}
