package com.stock.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DartAccount {
    @JsonProperty("rcept_no")
    private String rceptNo;
    
    @JsonProperty("reprt_code")
    private String reprtCode;
    
    @JsonProperty("bsns_year")
    private String bsnsYear;
    
    @JsonProperty("corp_code")
    private String corpCode;
    
    @JsonProperty("sj_div")
    private String sjDiv;  // 재무제표구분 (BS, IS, CF)
    
    @JsonProperty("sj_nm")
    private String sjNm;   // 재무제표명
    
    @JsonProperty("account_id")
    private String accountId;
    
    @JsonProperty("account_nm")
    private String accountNm;  // 계정명
    
    @JsonProperty("account_detail")
    private String accountDetail;
    
    @JsonProperty("thstrm_nm")
    private String thstrmNm;  // 당기명
    
    @JsonProperty("thstrm_amount")
    private String thstrmAmount;  // 당기금액
    
    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;  // 전기명
    
    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;  // 전기금액
    
    @JsonProperty("bfefrmtrm_nm")
    private String bfefrmtrmNm;  // 전전기명
    
    @JsonProperty("bfefrmtrm_amount")
    private String bfefrmtrmAmount;  // 전전기금액
    
    @JsonProperty("ord")
    private String ord;
    
    @JsonProperty("currency")
    private String currency;
}
