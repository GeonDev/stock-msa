package com.stock.strategy.dto;

import com.stock.strategy.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "매매 주문 정보")
public class TradeOrder {
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "주문 유형 (BUY, SELL)", example = "BUY")
    private OrderType orderType;

    @Schema(description = "주문 수량", example = "10")
    private Integer quantity;

    @Schema(description = "주문 가격", example = "71000")
    private BigDecimal price;

    @Schema(description = "주문 날짜", example = "2023-01-15")
    private LocalDate orderDate;
}
