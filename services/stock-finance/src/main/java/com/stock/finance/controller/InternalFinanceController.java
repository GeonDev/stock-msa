package com.stock.finance.controller;

import com.stock.common.dto.CorpFinanceIndicatorDto;
import com.stock.finance.mapper.CorpFinanceIndicatorMapper;
import com.stock.finance.repository.CorpFinanceIndicatorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/internal")
@Tag(name = "Internal Finance API", description = "서비스 간 통신을 위한 재무 데이터 내부 API")
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
}
