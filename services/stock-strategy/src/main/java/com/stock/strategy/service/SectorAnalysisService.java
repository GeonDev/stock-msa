package com.stock.strategy.service;

import com.stock.common.dto.StockIndicatorDto;
import com.stock.strategy.client.PriceClient;
import com.stock.strategy.entity.SectorAnalysis;
import com.stock.strategy.repository.SectorAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorAnalysisService {

    private final SectorAnalysisRepository sectorAnalysisRepository;
    private final PriceClient priceClient;

    // TODO: In a real system, you'd fetch mapping from stock-corp or DB
    // Here we'll mock a simple sector mapping or assume 'universe' is grouped by sector beforehand
    // For MVP phase 3, we can assign stocks to arbitrary sectors or fetch mapping if available.

    @Transactional
    public List<SectorAnalysis> analyzeAndSaveSectors(LocalDate date, Map<String, List<String>> sectorToStocks, String dateStr) {
        List<SectorAnalysis> results = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : sectorToStocks.entrySet()) {
            String sectorName = entry.getKey();
            List<String> stocks = entry.getValue();
            
            if (stocks.isEmpty()) continue;

            List<StockIndicatorDto> indicators = priceClient.getIndicatorsByDateBatch(stocks, dateStr);

            double sumMomentum = 0;
            int count = 0;
            for (StockIndicatorDto ind : indicators) {
                if (ind.getMomentum6m() != null) {
                    sumMomentum += ind.getMomentum6m().doubleValue();
                    count++;
                }
            }

            if (count > 0) {
                double avgMomentum = sumMomentum / count;
                // Relative strength is simple alpha over some baseline, here we just use absolute avg momentum for simplicity
                BigDecimal avgMomBd = BigDecimal.valueOf(avgMomentum).setScale(4, RoundingMode.HALF_UP);
                
                SectorAnalysis analysis = SectorAnalysis.builder()
                        .sectorName(sectorName)
                        .analysisDate(date)
                        .avgMomentum12m(avgMomBd)
                        .relativeStrength(avgMomBd) // simplified
                        .build();
                
                results.add(analysis);
            }
        }

        return sectorAnalysisRepository.saveAll(results);
    }
}
