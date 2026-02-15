package com.stock.finance.controller;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.finance.mapper.CorpFinanceIndicatorMapper;
import com.stock.finance.repository.CorpFinanceIndicatorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Finance API", description = "서비스 간 통신용 재무 지표 API")
public class InternalFinanceController {

    private final CorpFinanceIndicatorRepository indicatorRepository;
    private final CorpFinanceIndicatorMapper mapper;

    @GetMapping("/indicators/batch")
    @Operation(summary = "지정된 종목 및 날짜의 재무 지표 조회", description = "여러 종목의 특정 날짜 재무 지표를 한 번에 조회합니다.")
    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(
            @RequestParam List<String> corpCodes,
            @RequestParam String date) {
        
        LocalDate basDt = LocalDate.parse(date);
        
        return mapper.toDtoList(
            indicatorRepository.findByCorpCodeInAndBasDt(corpCodes, basDt)
        );
    }

    @GetMapping("/indicators/{corpCode}/latest")
    @Operation(summary = "최신 재무 지표 조회", description = "특정 종목의 가장 최근 재무 지표를 조회합니다.")
    public CorpFinanceIndicatorDto getLatestIndicator(@PathVariable String corpCode) {
        return indicatorRepository.findTopByCorpCodeOrderByBasDtDesc(corpCode)
                .map(mapper::toDto)
                .orElse(null);
    }
}
