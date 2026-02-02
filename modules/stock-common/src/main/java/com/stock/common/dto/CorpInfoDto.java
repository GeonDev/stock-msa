package com.stock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CorpInfoDto {
    private String corpCode;
    private String corpName;
    private String stockCode;
    private String isinCode;
    private LocalDate checkDt;
}
