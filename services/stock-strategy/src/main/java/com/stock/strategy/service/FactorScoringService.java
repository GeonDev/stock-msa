package com.stock.strategy.service;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.common.dto.StockIndicatorDto;
import com.stock.strategy.entity.FactorScore;
import com.stock.strategy.repository.FactorScoreRepository;
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
public class FactorScoringService {

    private final FactorScoreRepository factorScoreRepository;

    @Transactional
    public List<FactorScore> calculateAndSaveScores(LocalDate date, 
                                                    List<CorpFinanceIndicatorDto> finances, 
                                                    List<StockIndicatorDto> indicators,
                                                    BigDecimal valueWeight,
                                                    BigDecimal momentumWeight,
                                                    BigDecimal qualityWeight) {
        
        Map<String, CorpFinanceIndicatorDto> financeMap = finances.stream()
                .collect(Collectors.toMap(CorpFinanceIndicatorDto::getCorpCode, f -> f, (f1, f2) -> f1));
        
        Map<String, StockIndicatorDto> indicatorMap = indicators.stream()
                .filter(i -> i.getStockCode() != null)
                .collect(Collectors.toMap(StockIndicatorDto::getStockCode, i -> i, (i1, i2) -> i1));

        Set<String> commonStocks = new HashSet<>(financeMap.keySet());
        commonStocks.retainAll(indicatorMap.keySet());

        List<String> validStocks = new ArrayList<>();
        List<Double> perList = new ArrayList<>();
        List<Double> pbrList = new ArrayList<>();
        List<Double> roeList = new ArrayList<>();
        List<Double> momList = new ArrayList<>();

        for (String stock : commonStocks) {
            CorpFinanceIndicatorDto f = financeMap.get(stock);
            StockIndicatorDto i = indicatorMap.get(stock);
            
            if (f.getPer() != null && f.getPbr() != null && f.getRoe() != null && i.getMomentum6m() != null) {
                validStocks.add(stock);
                perList.add(f.getPer().doubleValue());
                pbrList.add(f.getPbr().doubleValue());
                roeList.add(f.getRoe().doubleValue());
                momList.add(i.getMomentum6m().doubleValue());
            }
        }

        if (validStocks.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate means and stddevs
        Stat perStat = calculateStat(perList);
        Stat pbrStat = calculateStat(pbrList);
        Stat roeStat = calculateStat(roeList);
        Stat momStat = calculateStat(momList);

        List<FactorScore> factorScores = new ArrayList<>();

        for (int i = 0; i < validStocks.size(); i++) {
            String stock = validStocks.get(i);
            
            // Value: PER & PBR (lower is better, so negate z-score)
            double perZ = -calculateZScore(perList.get(i), perStat);
            double pbrZ = -calculateZScore(pbrList.get(i), pbrStat);
            double valueZ = (perZ + pbrZ) / 2.0;

            // Quality: ROE (higher is better)
            double qualityZ = calculateZScore(roeList.get(i), roeStat);

            // Momentum: 6M Momentum (higher is better)
            double momZ = calculateZScore(momList.get(i), momStat);

            // Total Score
            double totalZ = valueZ * valueWeight.doubleValue() + 
                            momZ * momentumWeight.doubleValue() + 
                            qualityZ * qualityWeight.doubleValue();

            FactorScore score = FactorScore.builder()
                    .stockCode(stock)
                    .scoreDate(date)
                    .valueScore(BigDecimal.valueOf(valueZ).setScale(4, RoundingMode.HALF_UP))
                    .momentumScore(BigDecimal.valueOf(momZ).setScale(4, RoundingMode.HALF_UP))
                    .qualityScore(BigDecimal.valueOf(qualityZ).setScale(4, RoundingMode.HALF_UP))
                    .totalScore(BigDecimal.valueOf(totalZ).setScale(4, RoundingMode.HALF_UP))
                    .build();
            
            factorScores.add(score);
        }

        return factorScoreRepository.saveAll(factorScores);
    }

    private double calculateZScore(double value, Stat stat) {
        if (stat.stdDev == 0) return 0.0;
        
        // Winsorization (cap at +/- 3 stddev)
        double z = (value - stat.mean) / stat.stdDev;
        if (z > 3.0) return 3.0;
        if (z < -3.0) return -3.0;
        
        return z;
    }

    private Stat calculateStat(List<Double> values) {
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.size();

        double sqSum = 0.0;
        for (double v : values) sqSum += Math.pow(v - mean, 2);
        double stdDev = Math.sqrt(sqSum / values.size());

        return new Stat(mean, stdDev);
    }

    private record Stat(double mean, double stdDev) {}
}
