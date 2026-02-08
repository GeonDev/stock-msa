package com.stock.corp.controller;

import com.stock.common.dto.CorpInfoDto;
import com.stock.corp.entity.CorpInfo;
import com.stock.corp.service.CorpInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/corp/internal")
public class CorpInternalController {

    private final CorpInfoService corpInfoService;

    @GetMapping("/{corpCode}")
    public CorpInfoDto getCorpInfoByCorpCode(@PathVariable String corpCode) {
        return corpInfoService.getCorpInfoByCorpCode(corpCode);
    }

    @GetMapping("/corps")
    public List<CorpInfoDto> getCorpsByMarket(@RequestParam String market) {
        return corpInfoService.getCorpsByMarket(market);
    }

    @GetMapping("/valid-codes")
    public Set<String> getValidCorpCodes() {
        return corpInfoService.getAllValidCorpInfos().stream()
                .map(CorpInfoDto::getCorpCode)
                .collect(Collectors.toSet());
    }
}
