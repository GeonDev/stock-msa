package com.stock.batch.repository;

import com.stock.batch.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    List<StockPrice> findTop200ByStockCodeAndBasDtBeforeOrderByBasDtDesc(String stockCode, LocalDate basDt);
    Optional<StockPrice> findFirstByStockCodeOrderByBasDtDesc(String stockCode);
    List<StockPrice> findByStockCodeOrderByBasDtAsc(String stockCode);
}
