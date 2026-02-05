package com.stock.price.service;

import com.stock.price.entity.StockIndicator;
import com.stock.price.entity.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalIndicatorService {

    /**
     * 특정 주식 데이터(targetPrice)에 대한 지표를 계산하여 StockIndicator를 업데이트합니다.
     * @param targetPrice 지표를 계산할 당일 데이터
     * @param historyPrices 과거 데이터 (최소 250개 이상 권장)
     */
    public void calculateAndFillIndicators(StockPrice targetPrice, List<StockPrice> historyPrices) {
        // 1. 과거 데이터와 현재 데이터 병합 및 정렬 (과거 -> 현재 순)
        List<StockPrice> allPrices = new java.util.ArrayList<>(List.copyOf(historyPrices));
        allPrices.add(targetPrice);
        allPrices.sort(Comparator.comparing(StockPrice::getBasDt));

        // 2. BarSeries 생성
        BarSeries series = createBarSeries(allPrices);
        int endIndex = series.getEndIndex();

        // 3. 지표 계산 준비
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 4. 지표 계산 및 매핑
        StockIndicator indicator = targetPrice.getStockIndicator();
        if (indicator == null) {
            indicator = new StockIndicator();
            indicator.setStockPrice(targetPrice);
            indicator.setId(targetPrice.getId());
            targetPrice.setStockIndicator(indicator);
        }

        // 이동평균선 (MA)
        indicator.setMa5(calculateSMA(closePrice, 5, endIndex));
        indicator.setMa20(calculateSMA(closePrice, 20, endIndex));
        indicator.setMa60(calculateSMA(closePrice, 60, endIndex));
        indicator.setMa120(calculateSMA(closePrice, 120, endIndex));
        indicator.setMa200(calculateSMA(closePrice, 200, endIndex));
        indicator.setMa250(calculateSMA(closePrice, 250, endIndex));

        // RSI (14)
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        indicator.setRsi14(getValue(rsi, endIndex));

        // Bollinger Bands (20, 2)
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, 20));
        StandardDeviationIndicator sd20 = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsUpperIndicator bbu = new BollingerBandsUpperIndicator(bbMiddle, sd20, series.numFactory().numOf(2));
        BollingerBandsLowerIndicator bbl = new BollingerBandsLowerIndicator(bbMiddle, sd20, series.numFactory().numOf(2));
        indicator.setBollingerUpper(getValue(bbu, endIndex));
        indicator.setBollingerLower(getValue(bbl, endIndex));

        // MACD (12, 26, 9)
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        indicator.setMacd(getValue(macd, endIndex));
        indicator.setMacdSignal(getValue(macdSignal, endIndex));
        
        // Momentum (기존 로직 유지 또는 Ta4j ROCIndicator로 대체 가능. 현재는 기존 필드 유지)
        // indicator.setMomentum1m(...) -> Ta4j ROCIndicator(closePrice, 20) 활용 가능
    }

    private BarSeries createBarSeries(List<StockPrice> prices) {
        BarSeries series = new BaseBarSeriesBuilder().withName(prices.get(0).getStockCode()).build();
        for (StockPrice p : prices) {
            ZonedDateTime zdt = p.getBasDt().atStartOfDay(ZoneId.of("Asia/Seoul"));
            // 수정주가가 있으면 우선 사용, 없으면 종가 사용
            BigDecimal close = p.getAdjClosePrice() != null ? 
                           p.getAdjClosePrice() : 
                           p.getEndPrice();
            
            // Ta4j Bar 추가: date, open, high, low, close, volume
            series.addBar(series.barBuilder()
                    .timePeriod(Duration.ofDays(1))
                    .endTime(zdt.toInstant())
                    .openPrice(series.numFactory().numOf(p.getStartPrice()))
                    .highPrice(series.numFactory().numOf(p.getHighPrice()))
                    .lowPrice(series.numFactory().numOf(p.getLowPrice()))
                    .closePrice(series.numFactory().numOf(close))
                    .volume(series.numFactory().numOf(p.getVolume() != null ? p.getVolume() : BigDecimal.ZERO))
                    .build());
        }
        return series;
    }

    private BigDecimal calculateSMA(ClosePriceIndicator indicator, int barCount, int index) {
        if (index < barCount - 1) return null; // 데이터 부족 시 null
        SMAIndicator sma = new SMAIndicator(indicator, barCount);
        return getValue(sma, index);
    }

    private BigDecimal getValue(org.ta4j.core.Indicator<Num> indicator, int index) {
        try {
            return new BigDecimal(indicator.getValue(index).toString());
        } catch (Exception e) {
            // 인덱스 범위 초과 등 예외 발생 시 null 처리
            return null;
        }
    }
}
