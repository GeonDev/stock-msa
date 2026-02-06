package com.stock.strategy.repository;

import com.stock.strategy.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {
    Optional<BacktestResult> findBySimulationId(Long simulationId);
}
