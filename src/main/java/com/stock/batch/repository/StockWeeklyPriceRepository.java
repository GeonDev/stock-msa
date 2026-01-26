package com.stock.batch.repository;

import com.stock.batch.entity.StockWeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockWeeklyPriceRepository extends JpaRepository<StockWeeklyPrice, Long> {
    void deleteByEndDateBetween(LocalDate startDate, LocalDate endDate);
    void deleteByStockCodeInAndEndDateBetween(List<String> stockCodes, LocalDate startDate, LocalDate endDate);
    List<StockWeeklyPrice> findByStockCodeInAndEndDateBetween(List<String> stockCodes, LocalDate startDate, LocalDate endDate);
}
