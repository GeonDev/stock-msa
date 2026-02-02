package com.stock.stock.repository;

import com.stock.stock.entity.StockWeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockWeeklyPriceRepository extends JpaRepository<StockWeeklyPrice, Long> {
    List<StockWeeklyPrice> findByStockCodeInAndEndDateBetween(List<String> stockCodes, LocalDate startDate, LocalDate endDate);
}
