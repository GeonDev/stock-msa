package com.stock.strategy.repository;

import com.stock.strategy.entity.BacktestSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestSimulationRepository extends JpaRepository<BacktestSimulation, Long> {
}
