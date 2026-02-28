package com.stock.price.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_STOCK_INDICATOR")
public class StockIndicator implements Serializable  {

    @Id
    @Column(name = "stock_price_id")
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "stock_price_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockPrice stockPrice;

    @Column(name = "ma5")
    private BigDecimal ma5;
    @Column(name = "ma20")
    private BigDecimal ma20;
    @Column(name = "ma60")
    private BigDecimal ma60;
    @Column(name = "ma120")
    private BigDecimal ma120;
    @Column(name = "ma200")
    private BigDecimal ma200;
    @Column(name = "ma250")
    private BigDecimal ma250;

    @Column(name = "momentum1m")
    private BigDecimal momentum1m;
    @Column(name = "momentum3m")
    private BigDecimal momentum3m;
    @Column(name = "momentum6m")
    private BigDecimal momentum6m;

    @Column(name = "rsi14")
    private BigDecimal rsi14;

    @Column(name = "bollinger_upper")
    private BigDecimal bollingerUpper;

    @Column(name = "bollinger_lower")
    private BigDecimal bollingerLower;

    @Column(name = "macd")
    private BigDecimal macd;

    @Column(name = "macd_signal")
    private BigDecimal macdSignal;
}
