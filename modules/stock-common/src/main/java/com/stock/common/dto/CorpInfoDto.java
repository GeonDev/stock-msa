package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String market;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkDt;
}
