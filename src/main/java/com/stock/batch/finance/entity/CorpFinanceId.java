package com.stock.batch.finance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorpFinanceId implements Serializable {
    private String corpCode;
    private LocalDate basDt;
}
