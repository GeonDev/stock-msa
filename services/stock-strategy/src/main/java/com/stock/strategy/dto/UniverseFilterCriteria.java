package com.stock.strategy.dto;

import com.stock.common.enums.StockMarket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "유니버스 필터링 조건")
public class UniverseFilterCriteria {
    @Schema(description = "대상 시장 (KOSPI, KOSDAQ 등)", example = "KOSPI")
    private StockMarket market;

    @Min(value = 0, message = "최소 시가총액은 0 이상이어야 합니다")
    @Schema(description = "최소 시가총액 (억 원 단위)", example = "1000")
    private Long minMarketCap;

    @Min(value = 0, message = "최대 시가총액은 0 이상이어야 합니다")
    @Schema(description = "최대 시가총액 (억 원 단위)", example = "1000000")
    private Long maxMarketCap;

    @DecimalMin(value = "0.0", message = "최소 시가총액 분위수는 0 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "최소 시가총액 분위수는 100 이하여야 합니다")
    @Schema(description = "최소 시가총액 분위수 (0-100, 0: 가장 작은 종목)", example = "0")
    private Double minMarketCapPercentile;

    @DecimalMin(value = "0.0", message = "최대 시가총액 분위수는 0 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "최대 시가총액 분위수는 100 이하여야 합니다")
    @Schema(description = "최대 시가총액 분위수 (0-100, 100: 가장 큰 종목)", example = "20")
    private Double maxMarketCapPercentile;

    @Schema(description = "제외 업종 리스트", example = "[\"금융\", \"지주사\"]")
    private List<String> excludeSectors;

    @Min(value = 0, message = "최소 평균 거래량은 0 이상이어야 합니다")
    @Schema(description = "최소 평균 거래량", example = "100000")
    private Long minTradingVolume;

    @Valid
    @Schema(description = "상세 지표 필터 조건 (PER, PBR, ROE 등)")
    private CustomFilterCriteria customFilter;
}
