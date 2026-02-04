package com.stock.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "stock_price_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockPrice stockPrice;

    @Column(name = "ma5")
    private Double ma5;
    @Column(name = "ma20")
    private Double ma20;
    @Column(name = "ma60")
    private Double ma60;
    @Column(name = "ma120")
    private Double ma120;
    @Column(name = "ma200")
    private Double ma200;
    @Column(name = "ma250")
    private Double ma250;

    @Column(name = "momentum1m")
    private Double momentum1m;
    @Column(name = "momentum3m")
    private Double momentum3m;
    @Column(name = "momentum6m")
    private Double momentum6m;
}
