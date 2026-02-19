package com.stock.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "서비스 정보 응답 DTO")
public class ServiceInfoDto {
    @Schema(description = "서비스 명칭", example = "stock-corp")
    private String serviceName;

    @Schema(description = "서비스 버전", example = "0.0.1-SNAPSHOT")
    private String version;

    @Schema(description = "활성 프로파일", example = "local")
    private String profiles;

}
