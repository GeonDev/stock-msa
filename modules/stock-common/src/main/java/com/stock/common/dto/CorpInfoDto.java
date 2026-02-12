package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "기업 정보 DTO")
public class CorpInfoDto {
    @Schema(description = "고유번호 (DART)", example = "00126380")
    private String corpCode;

    @Schema(description = "법인명", example = "삼성전자")
    private String corpName;

    @Schema(description = "종목코드 (상장사)", example = "005930")
    private String stockCode;

    @Schema(description = "ISIN 코드", example = "KR7005930003")
    private String isinCode;

    @Schema(description = "시장 구분 (KOSPI, KOSDAQ 등)", example = "KOSPI")
    private String market;

    @Schema(description = "업종 분류", example = "IT_HARDWARE")
    private com.stock.common.enums.SectorType sector;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "최종 확인일", example = "2024-02-12")
    private LocalDate checkDt;
}
