package com.stock.price.repository;

import com.stock.price.entity.StockWeeklyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockWeeklyPriceRepository extends JpaRepository<StockWeeklyPrice, Long> {
    List<StockWeeklyPrice> findByStockCodeInAndEndDateBetween(List<String> stockCodes, LocalDate startDate, LocalDate endDate);
    Optional<StockWeeklyPrice> findByStockCodeAndStartDateAndEndDate(String stockCode, LocalDate startDate, LocalDate endDate);
}
