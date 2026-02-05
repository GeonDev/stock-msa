package com.stock.price.service;

import com.stock.common.dto.StockIssuanceInfoDto;
import com.stock.common.enums.CorpEventType;
import com.stock.price.client.StockEventClient;
import com.stock.price.entity.CorpEventHistory;
import com.stock.price.repository.CorpEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorpEventService {

    private final StockEventClient stockEventClient;
    private final CorpEventRepository corpEventRepository;

    @Transactional
    public void collectAndSaveEvents(String stockCode) {
        // 1. API 호출
        List<StockIssuanceInfoDto> issuanceInfos = stockEventClient.getIssuanceInfo(stockCode);

        // 2. 엔티티 변환 및 저장
        for (StockIssuanceInfoDto info : issuanceInfos) {
            CorpEventType eventType = mapEventType(info.getEventType());
            if (eventType == null) continue; // 처리하지 않는 이벤트 타입은 스킵

            // 중복 확인
            if (corpEventRepository.existsByStockCodeAndEventDateAndEventType(
                    stockCode, info.getEventDate(), eventType)) {
                continue;
            }

            CorpEventHistory history = CorpEventHistory.builder()
                    .stockCode(stockCode)
                    .eventDate(info.getEventDate())
                    .eventType(eventType)
                    .ratio(info.getIssuanceRatio()) // API가 주는 배정비율 그대로 저장 (계산 시 변환)
                    .amount(info.getIssuanceAmount())
                    .description(info.getEventType())
                    .build();

            corpEventRepository.save(history);
        }
    }

    private CorpEventType mapEventType(String ksdEventName) {
        if (ksdEventName == null) return null;
        if (ksdEventName.contains("유상증자")) return CorpEventType.PAID_INCREASE;
        if (ksdEventName.contains("무상증자")) return CorpEventType.FREE_INCREASE;
        if (ksdEventName.contains("액면분할") || ksdEventName.contains("주식분할")) return CorpEventType.STOCK_SPLIT;
        if (ksdEventName.contains("합병")) return CorpEventType.STOCK_MERGER;
        if (ksdEventName.contains("배당")) return CorpEventType.DIVIDEND;
        if (ksdEventName.contains("감자")) return CorpEventType.CAPITAL_REDUCTION;
        return null;
    }
}
