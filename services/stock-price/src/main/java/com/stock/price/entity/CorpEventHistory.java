package com.stock.price.entity;

import com.stock.common.enums.CorpEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_CORP_EVENT_HISTORY")
public class CorpEventHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "event_date")
    private String eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private CorpEventType eventType;

    @Column(name = "ratio")
    private BigDecimal ratio;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "description")
    private String description;
}
