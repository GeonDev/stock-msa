package com.stock.strategy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "포트폴리오 보유 종목 정보")
public class PortfolioHolding {
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "보유 수량", example = "100")
    private Integer quantity;

    @Schema(description = "평균 매수 단가", example = "70000")
    private BigDecimal averagePrice;

    @Schema(description = "현재가", example = "72000")
    private BigDecimal currentPrice;

    @Schema(description = "평가 금액", example = "7200000")
    private BigDecimal marketValue;
}
