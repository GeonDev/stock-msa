package com.stock.corp.controller;

import com.stock.corp.service.CorpInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Corp Info Public", description = "기업 정보 조회 공용 API")
@RestController
@RequestMapping("/api/v1/corp")
@RequiredArgsConstructor
public class CorpPublicController {

    private final CorpInfoService corpInfoService;

    @Operation(summary = "전체 유니버스 종목 수 조회")
    @GetMapping("/universe/count")
    public ResponseEntity<Long> getUniverseCount() {
        return ResponseEntity.ok(corpInfoService.getUniverseCount());
    }
}
