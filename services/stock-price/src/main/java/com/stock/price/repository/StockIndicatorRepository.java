package com.stock.price.repository;

import com.stock.price.entity.StockIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {
    
    @Query("SELECT i FROM StockIndicator i JOIN i.stockPrice s WHERE s.stockCode IN :stockCodes AND s.basDt = :basDt")
    List<StockIndicator> findByStockCodesAndBasDt(@Param("stockCodes") List<String> stockCodes, @Param("basDt") LocalDate basDt);
}
