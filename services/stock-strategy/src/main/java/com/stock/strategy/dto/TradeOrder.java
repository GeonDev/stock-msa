package com.stock.strategy.dto;

import com.stock.strategy.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeOrder {
    private String stockCode;
    private OrderType orderType;
    private Integer quantity;
    private BigDecimal price;
    private LocalDate orderDate;
}
