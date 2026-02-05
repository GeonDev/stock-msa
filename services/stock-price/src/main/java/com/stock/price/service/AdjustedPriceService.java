package com.stock.price.service;

import com.stock.common.enums.CorpEventType;
import com.stock.price.entity.CorpEventHistory;
import com.stock.price.entity.StockPrice;
import com.stock.price.repository.CorpEventRepository;
import com.stock.price.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustedPriceService {

    private final StockPriceRepository stockPriceRepository;
    private final CorpEventRepository corpEventRepository;

    @Transactional
    public void calculateAndSaveAdjustedPrices(String stockCode) {
        // 1. 전체 주가 데이터 조회 (최신순)
        List<StockPrice> prices = stockPriceRepository.findByStockCodeOrderByBasDtAsc(stockCode);
        if (prices.isEmpty()) return;

        // 2. 전체 이벤트 조회 (최신순)
        List<CorpEventHistory> events = corpEventRepository.findAllByStockCodeOrderByEventDateDesc(stockCode);
        
        // 시간순(과거->미래) 처리를 위해 리스트 뒤집기 또는 날짜 비교 로직 구현
        // 여기서는 "과거 데이터를 보정"하는 방식이므로, 최신->과거로 역순회하며 팩터를 누적 곱(Product)하는 방식이 효율적입니다.
        // 하지만 직관적인 이해를 위해 Ta4j등 일반적인 방식인 "Factor를 적용하여 재계산"하는 방식을 씁니다.
        
        // 간단한 전략:
        // 1. 기본 수정주가는 종가와 동일하게 초기화.
        // 2. 이벤트 발생일 이전의 모든 데이터에 대해 수정 계수를 적용.
        
        // 초기화
        for (StockPrice price : prices) {
            if (price.getAdjClosePrice() == null) {
                // endPrice is already BigDecimal
                price.setAdjClosePrice(price.getEndPrice());
            }
        }

        if (events.isEmpty()) return;

        for (CorpEventHistory event : events) {
            LocalDate eventDate = LocalDate.parse(event.getEventDate(), DateTimeFormatter.BASIC_ISO_DATE);
            BigDecimal factor = calculateAdjustmentFactor(event);

            if (factor.compareTo(BigDecimal.ONE) == 0) continue;

            // 이벤트 발생일 '이전'의 모든 주가에 Factor 적용
            for (StockPrice price : prices) {
                if (price.getBasDt().isBefore(eventDate)) {
                    BigDecimal currentAdj = price.getAdjClosePrice();
                    // 소수점 4자리 반올림
                    price.setAdjClosePrice(currentAdj.multiply(factor).setScale(4, java.math.RoundingMode.HALF_UP));
                }
            }
        }
        
        // 일괄 저장 (Batch Update 권장되나 JPA Dirty Checking 활용)
        // 성능 이슈 발생 시 JDBC Batch로 전환 필요
    }

    private BigDecimal calculateAdjustmentFactor(CorpEventHistory event) {
        BigDecimal ratio = event.getRatio() != null ? event.getRatio() : BigDecimal.ZERO;
        
        // 수정계수 산출 로직 (Phase 1 단순화 버전)
        // 수정주가 = 과거주가 * Factor
        // 예: 1/5 액면분할 -> 주가는 1/5토막 남 -> 과거 주가도 1/5로 줄여야 함 -> Factor = 0.2
        
        if (event.getEventType() == CorpEventType.STOCK_SPLIT) {
             // 액면분할: ratio가 보통 분할비율 (예: 5.0 -> 1주가 5주 됨)이라면 Factor는 1/5
             // 데이터 포털의 'ratio' 값이 어떤 포맷인지 확인 필요하지만, 보통 '발행비율' 등을 줌.
             // 여기서는 임시로 ratio > 1 이면 분할로 간주하여 역수 취함.
             if (ratio.compareTo(BigDecimal.ONE) > 0) {
                 return BigDecimal.ONE.divide(ratio, 8, java.math.RoundingMode.HALF_UP);
             }
        } 
        else if (event.getEventType() == CorpEventType.FREE_INCREASE) {
            // 무상증자: 1주당 0.5주 배정 -> ratio 0.5
            // 가치 희석: 1 / (1 + 0.5) = 1 / 1.5 = 0.666...
            return BigDecimal.ONE.divide(BigDecimal.ONE.add(ratio), 8, java.math.RoundingMode.HALF_UP);
        }
        // 유상증자 등은 복잡하므로 Phase 1에서는 Skip 하거나 1.0 처리
        
        return BigDecimal.ONE;
    }
}
