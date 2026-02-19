package com.stock.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DartCorpCode {
    @JsonProperty("corp_code")
    private String corpCode;  // 고유번호 (8자리)
    
    @JsonProperty("corp_name")
    private String corpName;  // 정식 명칭
    
    @JsonProperty("stock_code")
    private String stockCode;  // 종목코드 (6자리, 상장사만)
    
    @JsonProperty("modify_date")
    private String modifyDate;  // 최종변경일자
}
