package com.stock.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "에러 응답")
public class ErrorResponse {
    @Schema(description = "에러 발생 시각")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드")
    private int status;

    @Schema(description = "에러 유형")
    private String error;

    @Schema(description = "에러 메시지")
    private String message;

    @Schema(description = "상세 에러 정보 (필드별 검증 오류 등)")
    private Map<String, String> details;
}
