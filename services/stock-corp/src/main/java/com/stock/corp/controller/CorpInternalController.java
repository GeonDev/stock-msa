package com.stock.corp.controller;

import com.stock.common.dto.CorpInfoDto;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.service.CorpInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/corp/internal")
public class CorpInternalController {

    private final CorpInfoService corpInfoService;

    @GetMapping("/{corpCode}")
    public CorpInfoDto getCorpInfoByCorpCode(@PathVariable String corpCode) {
        CorpInfo entity = corpInfoService.getCorpInfoByCorpCode(corpCode);
        if (entity == null) return null;
        
        return CorpInfoDto.builder()
                .corpCode(entity.getCorpCode())
                .corpName(entity.getCorpName())
                .stockCode(entity.getStockCode())
                .isinCode(entity.getIsinCode())
                .checkDt(entity.getCheckDt())
                .build();
    }

    @GetMapping("/valid-codes")
    public Set<String> getValidCorpCodes() {
        return corpInfoService.getAllValidCorpInfos().stream()
                .map(CorpInfo::getCorpCode)
                .collect(Collectors.toSet());
    }
}
