package com.stock.batch.stock.entity;

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
    @Column(name = "STOCK_PRICE_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "STOCK_PRICE_ID", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private StockPrice stockPrice;

    private Double ma5;
    private Double ma20;
    private Double ma60;
    private Double ma120;
    private Double ma200;
    private Double ma250;

    private Double momentum1m;
    private Double momentum3m;
    private Double momentum6m;
}
