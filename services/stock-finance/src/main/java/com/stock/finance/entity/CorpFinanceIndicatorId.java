package com.stock.finance.entity;

import com.stock.common.enums.ReportCode;
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
public class CorpFinanceIndicatorId implements Serializable {
    private String corpCode;
    private LocalDate basDt;
    private ReportCode reportCode;
}
