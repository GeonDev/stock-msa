package com.stock.batch.repository;

import com.stock.batch.entity.StockIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {
}
