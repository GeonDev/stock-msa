package com.stock.batch.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_STOCK_MONTHLY_PRICE")
public class StockMonthlyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

    String stockCode;
    String marketCode;
    LocalDate startDate;
    LocalDate endDate;
    Integer volume;
    Long volumePrice;
    Integer startPrice;
    Integer endPrice;
    Integer highPrice;
    Integer lowPrice;
    Long stockTotalCnt;
    Long marketTotalAmt;
}
