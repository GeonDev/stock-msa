package com.stock.strategy.repository;

import com.stock.strategy.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    List<TradeHistory> findBySimulationIdOrderByTradeDateAsc(Long simulationId);
}
