package com.stock.strategy.dto;

import com.stock.common.enums.StockMarket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniverseFilterCriteria {
    private StockMarket market;
    private Long minMarketCap;
    private Long maxMarketCap;
    private List<String> excludeSectors;
    private Long minTradingVolume;
    private Map<String, Object> customConditions;
}
