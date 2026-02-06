package com.stock.strategy.repository;

import com.stock.strategy.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findBySimulationIdOrderBySnapshotDateAsc(Long simulationId);
    List<PortfolioSnapshot> findBySimulationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long simulationId, LocalDate startDate, LocalDate endDate);
}
