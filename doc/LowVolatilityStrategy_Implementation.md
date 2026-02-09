# LowVolatilityStrategy 구현 완료

## 구현 일자
2026-02-09

## 전략 개요
저변동성 전략은 변동성이 낮은 종목이 장기적으로 안정적인 수익을 제공한다는 "저변동성 이상 현상(Low Volatility Anomaly)"을 활용합니다.

## 핵심 로직

### 1. 변동성 계산
- **기간**: 최근 60일 (약 3개월)
- **방법**: 일별 수익률의 표준편차

```
일별 수익률 = (당일 종가 - 전일 종가) / 전일 종가
변동성 = √(Σ(수익률 - 평균수익률)² / N)
```

### 2. 종목 선정
- 유니버스 내 모든 종목의 변동성 계산
- 변동성 오름차순 정렬 (낮은 순)
- 하위 20개 종목 선정
- 동일 비중 배분

### 3. 리밸런싱
- 기존 보유 종목 중 하위 20개에 포함되지 않은 종목 매도
- 선정된 저변동성 종목 매수/매도로 목표 비중 달성

## 구현 세부사항

### 주요 상수
```java
private static final int TOP_N = 20;              // 선정 종목 수
private static final int VOLATILITY_PERIOD = 60;  // 변동성 계산 기간 (60일)
```

### 핵심 메서드

#### 1. `calculateVolatility()`
유니버스 내 모든 종목의 변동성 계산:
- 과거 60일 가격 데이터 조회
- 일별 수익률 계산
- 표준편차로 변동성 산출

#### 2. `calculateStandardDeviation()`
표준편차 계산:
- 평균 계산
- 분산 계산
- 제곱근으로 표준편차 도출

## 사용 예시

```json
POST /api/v1/strategy/backtest
{
  "strategyName": "LowVolatility",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000000,
  "rebalancingPeriod": "MONTHLY",
  "universeFilter": {
    "market": "KOSPI",
    "minMarketCap": 100000000000
  }
}
```

## 전략 특성

### 장점
- **안정성**: 변동성이 낮아 급격한 손실 위험 감소
- **방어적**: 하락장에서 상대적으로 강한 성과
- **장기 성과**: 장기적으로 시장 대비 초과 수익 가능

### 단점
- **상승장 약세**: 급등장에서 수익률 제한적
- **유동성**: 저변동성 종목은 거래량이 적을 수 있음
- **섹터 편중**: 특정 섹터(유틸리티, 필수소비재)에 집중될 수 있음

### 최적 사용 환경
- **시장 상황**: 변동성 높은 시장, 하락장
- **리밸런싱 주기**: 월간 또는 분기별
- **투자 성향**: 안정적 수익 추구, 위험 회피형

## 학술적 배경
- Baker, Bradley, & Wurgler (2011): "Benchmarks as Limits to Arbitrage"
- Ang, Hodrick, Xing, & Zhang (2006): "The Cross-Section of Volatility and Expected Returns"

## Phase 2 진행 상황
- ✅ EqualWeightStrategy
- ✅ MomentumStrategy
- ✅ **LowVolatilityStrategy** ← 금번 구현
- ⏳ ValueStrategy (남은 1개)
