package com.stock.batch.repository;

import com.stock.batch.entity.StockWeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockWeeklyPriceRepository extends JpaRepository<StockWeeklyPrice, Long> {
}
