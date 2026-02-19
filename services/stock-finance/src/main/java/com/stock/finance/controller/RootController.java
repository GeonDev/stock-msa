package com.stock.finance.controller;

import com.stock.common.dto.ServiceInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Root API", description = "서비스 정보 확인을 위한 루트 엔드포인트")
public class RootController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    @GetMapping("/")
    @Operation(summary = "서비스 정보 조회", description = "현재 구동 중인 서비스의 이름, 버전, 프로파일 정보를 조회합니다.")
    public ResponseEntity<ServiceInfoDto> getServiceInfo() {
        return ResponseEntity.ok(ServiceInfoDto.builder()
                .serviceName(serviceName)
                .version("0.0.1-SNAPSHOT")
                .profiles(activeProfiles)
                .build());
    }
}
