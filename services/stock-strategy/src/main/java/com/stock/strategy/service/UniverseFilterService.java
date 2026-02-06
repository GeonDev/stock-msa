package com.stock.strategy.service;

import com.stock.strategy.client.CorpClient;
import com.stock.strategy.dto.UniverseFilterCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseFilterService {

    private final CorpClient corpClient;

    public List<String> filter(LocalDate baseDate, UniverseFilterCriteria criteria) {
        if (criteria == null) {
            return new ArrayList<>();
        }

        try {
            // 시장별 종목 조회
            String market = criteria.getMarket() != null ? criteria.getMarket().name() : "KOSPI";
            String dateStr = baseDate.toString();
            
            var corps = corpClient.getCorpsByMarket(market, dateStr);
            
            // 필터링 로직 (추후 확장)
            return corps.stream()
                    .map(corp -> corp.getStockCode())
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to filter universe", e);
            return new ArrayList<>();
        }
    }
}
