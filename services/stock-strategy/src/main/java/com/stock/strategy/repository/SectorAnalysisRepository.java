package com.stock.strategy.repository;

import com.stock.strategy.entity.SectorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorAnalysisRepository extends JpaRepository<SectorAnalysis, Long> {
    List<SectorAnalysis> findByAnalysisDateOrderByAvgMomentum12mDesc(LocalDate analysisDate);
    Optional<SectorAnalysis> findBySectorNameAndAnalysisDate(String sectorName, LocalDate analysisDate);
}
