package com.stock.gateway.controller;

import com.stock.common.dto.ServiceInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RootController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    @GetMapping("/")
    public Mono<ResponseEntity<ServiceInfoDto>> getServiceInfo() {
        return Mono.just(ResponseEntity.ok(ServiceInfoDto.builder()
                .serviceName(serviceName)
                .version("0.0.1-SNAPSHOT")
                .profiles(activeProfiles)
                .build()));
    }
}
