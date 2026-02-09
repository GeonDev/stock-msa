# MomentumStrategy 구현 문서

## 개요
MomentumStrategy는 과거 수익률이 높은 종목이 단기적으로 지속 상승하는 경향(모멘텀 효과)을 활용하는 퀀트 투자 전략입니다.

## 전략 로직

### 1. 모멘텀 스코어 계산
각 종목에 대해 다음 3가지 기간의 모멘텀을 계산합니다:
- **1개월 모멘텀** (20일): 최근 20일간 수익률
- **3개월 모멘텀** (60일): 최근 60일간 수익률
- **6개월 모멘텀** (120일): 최근 120일간 수익률

모멘텀 계산 공식:
```
모멘텀 = ((현재가 - 과거가) / 과거가) × 100
```

### 2. 가중 평균 스코어
각 모멘텀에 가중치를 적용하여 최종 스코어를 산출합니다:
```
최종 스코어 = (1개월 모멘텀 × 0.5) + (3개월 모멘텀 × 0.3) + (6개월 모멘텀 × 0.2)
```

**가중치 설정 근거**:
- 1개월 모멘텀(50%): 최근 추세가 가장 중요
- 3개월 모멘텀(30%): 중기 추세 반영
- 6개월 모멘텀(20%): 장기 추세 참고

### 3. 종목 선정
- 유니버스 내 모든 종목의 모멘텀 스코어를 계산
- 스코어 기준 상위 20개 종목 선정
- 선정된 종목에 동일 비중 배분

### 4. 리밸런싱
- 기존 보유 종목 중 상위 20개에 포함되지 않은 종목은 전량 매도
- 상위 20개 종목에 대해 목표 비중에 맞춰 매수/매도

## 구현 세부사항

### 주요 상수
```java
private static final int TOP_N = 20;                    // 선정 종목 수
private static final int MOMENTUM_1M_DAYS = 20;         // 1개월 = 20 거래일
private static final int MOMENTUM_3M_DAYS = 60;         // 3개월 = 60 거래일
private static final int MOMENTUM_6M_DAYS = 120;        // 6개월 = 120 거래일

private static final BigDecimal WEIGHT_1M = new BigDecimal("0.5");  // 1개월 가중치
private static final BigDecimal WEIGHT_3M = new BigDecimal("0.3");  // 3개월 가중치
private static final BigDecimal WEIGHT_6M = new BigDecimal("0.2");  // 6개월 가중치
```

### 핵심 메서드

#### 1. `rebalance()`
리밸런싱 실행 메인 로직:
1. 유니버스 종목의 모멘텀 스코어 계산
2. 상위 N개 종목 선정
3. 기존 보유 종목 중 제외 대상 매도
4. 선정 종목 리밸런싱 (매수/매도)

#### 2. `calculateMomentumScores()`
유니버스 내 모든 종목의 모멘텀 스코어 계산:
- 과거 130일치 가격 데이터 조회 (6개월 + 버퍼)
- 각 종목별 1개월, 3개월, 6개월 모멘텀 계산
- 가중 평균으로 최종 스코어 산출

#### 3. `calculateMomentum()`
특정 기간의 모멘텀 계산:
- 시작일과 종료일의 가격 비교
- 수익률을 백분율로 환산
- 데이터 부족 시 null 반환

## 사용 예시

### 백테스팅 요청
```json
POST /api/v1/strategy/backtest
{
  "strategyName": "Momentum",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000000,
  "rebalancingPeriod": "MONTHLY",
  "tradingFeeRate": 0.00015,
  "taxRate": 0.0023,
  "universeFilter": {
    "market": "KOSPI",
    "minMarketCap": 100000000000
  }
}
```

### 전략 선택
```java
Strategy strategy = strategyFactory.getStrategy("Momentum");
List<TradeOrder> orders = strategy.rebalance(date, portfolio, universe);
```

## 성능 특성

### 장점
- **추세 추종**: 상승 추세 종목에 집중 투자
- **객관적 선정**: 정량적 지표 기반 종목 선정
- **자동 손절**: 모멘텀 약화 시 자동 매도

### 단점
- **변동성**: 시장 급변 시 큰 손실 가능
- **거래 비용**: 리밸런싱 빈도에 따라 수수료 부담
- **과거 의존**: 과거 수익률이 미래를 보장하지 않음

### 최적 사용 환경
- **시장 상황**: 상승장 또는 추세가 명확한 시장
- **리밸런싱 주기**: 월간 (MONTHLY) 권장
- **유니버스**: 유동성 높은 대형주 중심

## 개선 가능 사항

### 1. 변동성 조정
현재는 수익률만 고려하지만, 변동성을 함께 고려하여 위험 조정 수익률 기반 선정 가능:
```
조정 스코어 = 모멘텀 / 변동성
```

### 2. 섹터 분산
특정 섹터 집중을 방지하기 위한 섹터별 상한 설정:
```java
private static final int MAX_PER_SECTOR = 5;
```

### 3. 동적 가중치
시장 상황에 따라 가중치를 동적으로 조정:
- 상승장: 단기 모멘텀 가중치 증가
- 하락장: 장기 모멘텀 가중치 증가

### 4. 캐싱 최적화
가격 데이터 조회 시 캐싱 적용으로 성능 개선:
```java
@Cacheable(value = "priceHistory", key = "#stockCode + '_' + #startDate + '_' + #endDate")
public List<StockPriceDto> getPriceHistory(String stockCode, String startDate, String endDate)
```

## 테스트

### 단위 테스트
`MomentumStrategyTest.java`에서 다음 항목 검증:
- 전략 이름 반환
- 빈 유니버스 처리
- 정상 데이터로 매수 주문 생성

### 통합 테스트 (권장)
실제 백테스팅 실행으로 검증:
1. 2020-2023년 KOSPI 데이터로 백테스팅
2. CAGR, MDD, Sharpe Ratio 계산
3. EqualWeightStrategy와 성과 비교

## 참고 문헌
- Jegadeesh, N., & Titman, S. (1993). "Returns to Buying Winners and Selling Losers"
- Carhart, M. M. (1997). "On Persistence in Mutual Fund Performance"
- Asness, C. S., Moskowitz, T. J., & Pedersen, L. H. (2013). "Value and Momentum Everywhere"
