package com.stock.strategy.repository;

import com.stock.strategy.entity.FactorScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactorScoreRepository extends JpaRepository<FactorScore, Long> {
    List<FactorScore> findByScoreDateOrderByTotalScoreDesc(LocalDate scoreDate);
    Optional<FactorScore> findByStockCodeAndScoreDate(String stockCode, LocalDate scoreDate);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM FactorScore f WHERE f.scoreDate = :scoreDate")
    void deleteByScoreDate(LocalDate scoreDate);
}
