# 테스트 및 검증 가이드

> **목적**: Phase 0-2 시스템의 데이터 수집, 처리, 백테스팅 기능 검증
>
> **최종 업데이트**: 2026-02-22
> - 재무 데이터 배치 버그 수정 반영 (필터링 로직 오류)
> - 재무 데이터 분기별 개별 수집 API 추가 (`reportCode` 파라미터)
> - `CorpFinance` 엔티티 단순화 (복합키 → auto-increment PK)
> - `ReportCode` enum + `@Convert` 정상 적용

## 📋 목차
- [검증 계획](#검증-계획)
- [배치 실행 순서](#배치-실행-순서)
- [데이터 검증 쿼리](#데이터-검증-쿼리)
- [발견된 이슈](#발견된-이슈)

---

## 검증 계획

### Phase 0: 데이터 수집 검증

#### 1. 기업 정보 수집
- [ ] 전체 상장 기업 수집 (2,700+ 예상)
- [ ] DART Corp Code 자동 매핑 (90%+ 목표)
- [ ] 기업 상세 정보 (업종 분류)

#### 2. 주가 데이터 수집
- [ ] KOSPI / KOSDAQ / KONEX 시장
- [ ] 수정주가 계산 (액면분할/배당 조정)
- [ ] 기술적 지표 계산 (RSI, MACD, Bollinger Bands, Momentum)

#### 3. 재무 데이터 수집 (DART API)
- [ ] 분기별 개별 수집 가능 (Q1, SEMI, Q3, ANNUAL)
- [ ] 재무상태표 / 손익계산서 / 현금흐름표
- [ ] 계산 지표 (FCF, EBITDA)
- [ ] 재무 지표 (PER, PBR, ROE, ROA 등 10개)

### Phase 1: 데이터 품질 검증
- [ ] 대차대조표 등식 검증 (자산 = 부채 + 자본)
- [ ] VERIFIED 비율 90%+ 목표
- [ ] 재무 지표 계산 성공률 90%+ 목표

### Phase 2: 백테스팅 엔진 검증
- [ ] 단일 전략(Value 등) 정상 실행 및 지표(CAGR, MDD, Sharpe) 검증
- [ ] 거래 비용(슬리피지, 수수료, 세금) 모델링 정확성 검증 (오차율 0%)
- [ ] 거래 제약(단일 종목 최대 비중 20%, 최소 단위 1주) 동작 검증
- [ ] 전략 비교 API(Compare) 및 그리드 서치(Optimize) 병렬 수행 검증

### Phase 3: 고급 전략 검증
- [ ] Z-Score 기반 멀티팩터 정규화 및 스코어링 정확도 검증
- [ ] 섹터 로테이션 모멘텀 기반 상위 종목 교체 동작 검증
- [ ] 듀얼 모멘텀 발동 시 하락장에서 현금 100% 보유 전환 확인
- [ ] 리스크 패리티 (Inverse Volatility) 기반 포트폴리오 비중 차등 분배 확인

---

## 배치 실행 순서

> **⚠️ 중요**: 반드시 순서대로 실행. 각 배치는 이전 단계 데이터에 의존.
>
> **⚠️ 날짜**: 2024년 데이터 사용 권장 (`date=20241014`)
> - DART API는 재무제표 발표 후 1.5~3개월 후 제공
> - 2025년 데이터는 미발표 상태일 수 있음

### 환경 준비

```bash
cd /Users/kafa/IdeaProjects/stock-msa
docker-compose down -v
docker-compose up -d --build
sleep 180

# 서비스 상태 확인
docker exec stock-corp-db mysql -u corp_user -pcorp_pass -e "SELECT 1"
docker exec stock-finance-db mysql -u finance_user -pfinance_pass -e "SELECT 1"
docker exec stock-price-db mysql -u stock_user -pstock_pass -e "SELECT 1"
```

### Step 1. 기업 정보 수집

```bash
curl -X POST "http://localhost:8081/batch/corp-info?date=20241014"
sleep 180

# 검증
docker exec stock-corp-db mysql -u corp_user -pcorp_pass stock_corp -e "
SELECT COUNT(*) as total,
       COUNT(dart_corp_code) as with_dart_code,
       ROUND(COUNT(dart_corp_code) * 100.0 / COUNT(*), 2) as mapping_rate
FROM TB_CORP_INFO;"
```

**예상 결과**: total=2,694, mapping_rate=100.00

### Step 2. 업종 정보 수집

```bash
curl -X POST "http://localhost:8081/batch/corp-detail/sector-update"
sleep 60
```

### Step 3. 주가 데이터 수집 (수정주가 + 기술적 지표 자동 계산)

```bash
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20241014"
sleep 300

curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20241014"
sleep 300

curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20241014"
sleep 60

# 검증
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total_prices FROM TB_STOCK_PRICE WHERE bas_dt = '2024-10-14';"
```

**예상 결과**: 2,832건

### Step 4. 재무 데이터 수집 (DART API)

> **⚠️ DART API 일일 한도**: 10,000건/일. 한도 초과 시 분기별로 나눠서 실행.

**예상 응답**: Q1, SEMI, Q3, ANNUAL 4개 분기 재무 데이터 JSON

#### 4-2. 전체 수집 (분기별 개별 실행 권장)

DART API 일일 한도(10,000건)를 고려해 분기별로 나눠 실행합니다.
- 전체 실행: 약 2,694 × 4 = **10,776 API 호출** (한도 초과 위험)
- 분기별 실행: 약 2,694 × 1 = **2,694 API 호출** (약 7분)

```bash
# 분기별 개별 실행 (권장)
curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014&reportCode=ANNUAL"
sleep 420

curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014&reportCode=Q3"
sleep 420

curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014&reportCode=SEMI"
sleep 420

curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014&reportCode=Q1"
sleep 420

# 또는 전체 한번에 (한도 주의)
# curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014"
# sleep 1800

# 검증
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT report_code, COUNT(*) as count,
       COUNT(CASE WHEN validation_status = 'VERIFIED' THEN 1 END) as verified
FROM TB_CORP_FINANCE
GROUP BY report_code
ORDER BY report_code;"
```

**예상 결과** (분기별):
| report_code | count | verified |
|-------------|-------|----------|
| 11011 (ANNUAL) | 2,000+ | 90%+ |
| 11012 (SEMI)   | 2,000+ | 90%+ |
| 11013 (Q1)     | 2,000+ | 90%+ |
| 11014 (Q3)     | 2,000+ | 90%+ |

---

## 데이터 검증 쿼리

### 기업 정보
```sql
-- DART Corp Code 매핑률
SELECT COUNT(*) as total,
       COUNT(dart_corp_code) as mapped,
       ROUND(COUNT(dart_corp_code) * 100.0 / COUNT(*), 2) as rate
FROM TB_CORP_INFO;

-- 시장별 분포
SELECT market, COUNT(*) as count FROM TB_CORP_INFO GROUP BY market;
```

### 재무 데이터
```sql
-- 분기별 수집 현황
SELECT report_code, COUNT(*) as count,
       COUNT(CASE WHEN validation_status = 'VERIFIED' THEN 1 END) as verified
FROM TB_CORP_FINANCE
GROUP BY report_code;

-- 대차대조표 등식 위반 확인
SELECT corp_code, biz_year, report_code,
       (total_asset - total_debt - total_capital) as diff
FROM TB_CORP_FINANCE
WHERE ABS(total_asset - total_debt - total_capital) > 1000000
LIMIT 10;
```

### 재무 지표
```sql
-- 지표 계산 성공률
SELECT COUNT(*) as total,
       COUNT(per) as per_count,
       COUNT(roe) as roe_count,
       COUNT(roa) as roa_count
FROM TB_CORP_FINANCE_INDICATOR;
```

### 기술적 지표
```sql
SELECT COUNT(*) as total,
       COUNT(rsi) as rsi_count,
       COUNT(macd) as macd_count
FROM TB_STOCK_INDICATOR
WHERE bas_dt = '2024-10-14';
```

---

## 발견된 이슈

### ✅ 수정 완료 (2026-02-22)

#### 재무 데이터 DB 저장 0건 버그
**증상**: 배치 실행 후 `TB_CORP_FINANCE` 건수 0건

**원인**: `CorpFinanceItemReader`에서 `getValidCorpCodes()`(DART 고유번호 Set)와 `CorpFinance.corpCode`(stockCode)를 비교해 모든 항목이 필터링됨

**수정**:
```java
// Before: 형식 불일치로 전체 필터링
Set<String> validCorpCodes = corpClient.getValidCorpCodes(); // "00126380" 형식
list.stream().filter(f -> validCorpCodes.contains(f.getCorpCode())) // "A005930" 형식

// After: 필터링 제거 (getCorpFinanceFromDart()에서 이미 상장사만 조회)
corpIterator = list.iterator();
```

#### `ArrayIndexOutOfBoundsException` (validateFinanceStep)
**원인**: `@IdClass` 복합키의 `@Id` 필드에 `@Convert`가 적용되지 않아 Hibernate가 `ReportCode` enum을 ordinal(byte)로 읽음

**수정**: 복합키 제거 → `@GeneratedValue(IDENTITY)` auto-increment PK로 변경, `reportCode`는 일반 필드로 `@Convert` 정상 적용

### ✅ 수정 완료 (2026-02-16)

#### DART API HTTPS 이슈
```java
// Before
.scheme("http")
// After
.scheme("https")
```

#### Gateway Netty/Undertow 충돌
Spring Cloud Gateway는 Reactive(Netty) 기반이므로 `spring-boot-starter-undertow` 제거

### ⚠️ 알려진 제약사항

#### DART API 일일 한도 (10,000건/일)
- 전체 4개 분기 동시 수집 시 한도 초과 위험
- **대응**: `reportCode` 파라미터로 분기별 개별 실행
  ```bash
  curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014&reportCode=ANNUAL"
  ```

#### 기술적 지표 계산 (300일 히스토리 필요)
- 히스토리 부족 종목은 지표 계산 스킵
- 신규 상장 종목 등 일부 미계산 정상

#### DART API 013 에러
- 일부 기업은 해당 분기 데이터 미발표 → 정상
- 전체 수집 실패 시 코드 버그 의심

---

## 백테스팅 및 전략 검증 시나리오 (Phase 2, 3)

수집된 데이터(`TB_STOCK_PRICE`, `TB_CORP_FINANCE`, `TB_STOCK_INDICATOR`)를 기반으로 백테스팅 엔진과 고급 전략들이 올바르게 동작하는지 확인합니다.

### Step 5. 단일 전략 백테스팅 검증 (Value + 고정 슬리피지)
- **목적**: 거래 비용 및 매매 제약조건의 정확도 확인

```bash
curl -X POST "http://localhost:8084/api/v1/strategy/backtest" \
     -H "Content-Type: application/json" \
     -d '{
           "strategyType": "VALUE",
           "startDate": "2023-01-01",
           "endDate": "2023-12-31",
           "initialCapital": 10000000,
           "rebalancingPeriod": "MONTHLY",
           "tradingFeeRate": 0.0015,
           "taxRate": 0.002,
           "slippageType": "FIXED",
           "fixedSlippageRate": 0.002,
           "maxWeightPerStock": 0.2,
           "valueStrategyConfig": {
             "topN": 20,
             "perWeight": 0.3,
             "pbrWeight": 0.3,
             "roeWeight": 0.4
           }
         }'
```
- **검증**: 응답받은 `simulationId`를 통해 DB(`TB_BACKTEST_RESULT`) 및 Trade History 조회를 통해 슬리피지와 20% 최대 비중 제한이 적용되었는지 확인.

### Step 6. 고급 전략 검증 (Multi-Factor Z-Score)
- **목적**: Z-Score 기반 통계적 정규화 및 가중치 합산 전략 작동 확인

```bash
curl -X POST "http://localhost:8084/api/v1/strategy/backtest" \
     -H "Content-Type: application/json" \
     -d '{
           "strategyType": "MULTI_FACTOR",
           "startDate": "2023-01-01",
           "endDate": "2023-12-31",
           "initialCapital": 10000000,
           "rebalancingPeriod": "MONTHLY",
           "tradingFeeRate": 0.0015,
           "taxRate": 0.002,
           "multiFactorConfig": {
             "topN": 20,
             "valueWeight": 0.4,
             "momentumWeight": 0.3,
             "qualityWeight": 0.3
           }
         }'
```
- **검증**: `TB_FACTOR_SCORE` 테이블에서 `score_date` 별 각 종목의 `total_score` 및 Z-score(음수/양수) 분배 확인.

### Step 7. 동적 자산배분 검증 (Dual Momentum)
- **목적**: 시장의 전체 평균 모멘텀이 하락세일 때 현금 비중 100%로 회피하는 동작 확인

```bash
curl -X POST "http://localhost:8084/api/v1/strategy/backtest" \
     -H "Content-Type: application/json" \
     -d '{
           "strategyType": "DUAL_MOMENTUM",
           "startDate": "2022-01-01",
           "endDate": "2022-12-31",
           "initialCapital": 10000000,
           "rebalancingPeriod": "MONTHLY",
           "tradingFeeRate": 0.0015,
           "taxRate": 0.002,
           "assetAllocationConfig": {
             "useDualMomentum": true,
             "maxRiskAssetWeight": 1.0
           }
         }'
```
- **검증**: 2022년 하락장 기간 동안 특정 달의 포트폴리오 스냅샷(`TB_PORTFOLIO_SNAPSHOT`) 조회 시 주식 보유량이 0이고 `cash_balance`가 자산의 100%인지 확인.

### Step 8. 그리드 서치 및 전략 비교 (최적화)
- **목적**: 파라미터 조합 순회 자동화 및 결과 랭킹 매기기

```bash
# 1. 그리드 서치 실행 (Top N: 10, 20 / Weight: 0.5 스텝)
curl -X POST "http://localhost:8084/api/v1/strategy/backtest/optimize" \
     -H "Content-Type: application/json" \
     -d '{
           "minTopN": 10,
           "maxTopN": 20,
           "stepTopN": 10,
           "weightStep": 0.5,
           "baseRequest": {
             "strategyType": "VALUE",
             "startDate": "2023-01-01",
             "endDate": "2023-12-31",
             "initialCapital": 10000000,
             "rebalancingPeriod": "MONTHLY",
             "tradingFeeRate": 0.0015,
             "taxRate": 0.002
           }
         }'
```
- **검증**: 실행 후 `TB_BACKTEST_RESULT`에서 `is_optimized = true` 인 다수의 결과가 생성되었는지 확인.
- 생성된 결과의 ID들(예: 10,11,12)을 모아 `/api/v1/strategy/backtest/compare?resultIds=10,11,12` 로 호출하여 `bestCagrSimulationId`, `lowestMddSimulationId` 등의 도출 결과를 검증.
