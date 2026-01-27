package com.stock.batch.stock.repository;

import com.stock.batch.stock.entity.StockMonthlyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockMonthlyPriceRepository extends JpaRepository<StockMonthlyPrice, Long> {

    List<StockMonthlyPrice> findByStockCodeInAndEndDateBetween(List<String> stockCodes, LocalDate startDate, LocalDate endDate);
}
