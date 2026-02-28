package com.stock.price.repository;

import com.stock.price.entity.StockPrice;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    @Cacheable(value = "historyPriceCache", key = "#stockCode + ':' + #basDt.toString()")
    List<StockPrice> findTop300ByStockCodeAndBasDtBeforeOrderByBasDtDesc(String stockCode, LocalDate basDt);

    Optional<StockPrice> findFirstByStockCodeOrderByBasDtDesc(String stockCode);
    
    Optional<StockPrice> findByStockCodeAndBasDt(String stockCode, LocalDate basDt);

    List<StockPrice> findByStockCodeInAndBasDt(List<String> stockCodes, LocalDate basDt);

    List<StockPrice> findByStockCodeOrderByBasDtAsc(String stockCode);

    @Query("SELECT DISTINCT s.stockCode FROM StockPrice s WHERE s.basDt BETWEEN :startDate AND :endDate")
    List<String> findDistinctStockCodeByBasDtBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT s.stockCode FROM StockPrice s")
    List<String> findDistinctStockCodes();

    List<StockPrice> findByStockCodeAndBasDtBetweenOrderByBasDtAsc(String stockCode, LocalDate startDate, LocalDate endDate);

    List<StockPrice> findByBasDt(LocalDate basDt);
}
