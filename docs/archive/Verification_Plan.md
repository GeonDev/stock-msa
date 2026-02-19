# Stock-MSA 로직 검증 실행 계획

## 문서 개요

**목적**: Phase 0~2까지 개발된 기능들이 정상적으로 동작하는지 체계적으로 검증  
**대상**: 인프라, 데이터 수집, 백테스팅 엔진  
**예상 소요 시간**: 약 120분 (2시간)  
**최종 업데이트**: 2026-02-15 (DART API 전환 및 부가 데이터 검증 추가)

---

## 0단계: 테스트 환경 초기화 (5분)

### 0.1 Docker 볼륨 완전 삭제

**중요**: 모든 테스트는 깨끗한 상태에서 시작해야 정확한 검증이 가능합니다.

```bash
# 모든 컨테이너 중지 및 볼륨 삭제
cd /path/to/stock-msa
docker-compose down -v

# 볼륨 삭제 확인
docker volume ls | grep stock-msa
# 결과: 아무것도 출력되지 않아야 함
```

**검증 포인트:**
- ✅ 모든 컨테이너 중지
- ✅ 모든 볼륨 삭제 (corp-db, finance-db, price-db, strategy-db, batch-db)
- ✅ 기존 데이터 완전 제거

### 0.2 전체 재빌드 및 시작

```bash
# 전체 서비스 재빌드 및 시작
docker-compose up -d --build

# 서비스 시작 대기 (약 2-3분)
sleep 180

# 컨테이너 상태 확인
docker-compose ps
```

**검증 포인트:**
- ✅ 11개 컨테이너 모두 실행 중
- ✅ DB 컨테이너 healthy 상태
- ✅ Flyway 마이그레이션 자동 실행
- ✅ 빈 데이터베이스 스키마 생성

### 0.3 초기 스키마 확인

```bash
# 재무 정보 테이블 스키마 확인
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SHOW TABLES;
SELECT COUNT(*) as row_count FROM TB_CORP_FINANCE;
SELECT COUNT(*) as row_count FROM TB_CORP_FINANCE_INDICATOR;
"

# 예상 결과: 테이블 존재, row_count = 0
```

**검증 포인트:**
- ✅ 모든 테이블 생성 완료
- ✅ 데이터 0건 (깨끗한 상태)
- ✅ Primary Key 및 Index 생성 확인

---

## 1단계: 인프라 및 서비스 상태 확인 (5분)

### 1.1 Docker 컨테이너 상태 확인

```bash
# 모든 컨테이너 상태 확인
docker-compose ps

# 예상 결과: 모든 서비스가 'Up (healthy)' 상태
```

**검증 포인트:**
- ✅ 11개 컨테이너 모두 실행 중
- ✅ DB 컨테이너 healthy 상태
- ✅ 서비스 컨테이너 정상 기동

### 1.2 서비스 헬스체크

```bash
# Eureka Dashboard 접속
curl -u $EUREKA_USER:$EUREKA_PASSWORD http://localhost:8761

# Gateway 헬스체크
curl -u $GATEWAY_USER:$GATEWAY_PASSWORD http://localhost:8080/actuator/health

# 각 도메인 서비스 직접 확인 (Actuator 엔드포인트 이슈 시)
curl http://localhost:8081/  # stock-corp
curl http://localhost:8082/  # stock-finance
curl http://localhost:8083/  # stock-price
curl http://localhost:8084/  # stock-strategy
```

**검증 포인트:**
- ✅ Eureka에 모든 서비스 등록
- ✅ Gateway 라우팅 정상
- ✅ 각 서비스 응답 정상

### 1.3 데이터베이스 연결 확인

```bash
# 각 DB 접속 테스트
docker exec stock-corp-db mysql -u corp_user -pcorp_pass -e "SELECT 1 as test;"
docker exec stock-finance-db mysql -u finance_user -pfinance_pass -e "SELECT 1 as test;"
docker exec stock-price-db mysql -u stock_user -pstock_pass -e "SELECT 1 as test;"
docker exec stock-strategy-db mysql -u strategy_user -pstrategy_pass -e "SELECT 1 as test;"
docker exec stock-batch-db mysql -u batch_user -pbatch_pass -e "SELECT 1 as test;"
```

**검증 포인트:**
- ✅ 모든 DB 연결 성공
- ✅ 계정 권한 정상
- ✅ 스키마 존재 확인

---

## 2단계: 데이터 수집 검증 (Phase 0 & 1) (20분)

### 2.1 기업 정보 수집

```bash
# 기업 정보 수집 배치 실행
curl -X POST "http://localhost:8081/batch/corp-info?date=20240213"

# 결과 확인 (배치 완료까지 약 2-3분 소요)
sleep 180
docker exec stock-corp-db mysql -u corp_user -pcorp_pass stock_corp -e "
SELECT COUNT(*) as total_corps FROM TB_CORP_INFO;
SELECT market, COUNT(*) as count FROM TB_CORP_INFO GROUP BY market;
SELECT stock_code, corp_name, market FROM TB_CORP_INFO LIMIT 5;
"
```

**검증 포인트:**
- ✅ 2,500개 이상의 기업 정보 수집
- ✅ Stock code 형식: `A900100` (A 접두사 포함)
- ✅ 시장 구분: KOSPI, KOSDAQ, KONEX
- ✅ 필수 필드 존재 (corp_name, stock_code, market)

### 2.1.1 기업 상세 정보 수집 (부가 데이터)

```bash
# 업종 정보 수집 배치 실행 (OpenDART API)
curl -X POST "http://localhost:8081/batch/corp-detail/sector-update"

# 결과 확인 (배치 완료까지 약 3-5분 소요)
sleep 300
docker exec stock-corp-db mysql -u corp_user -pcorp_pass stock_corp -e "
SELECT COUNT(*) as total_with_sector FROM TB_CORP_INFO WHERE industry IS NOT NULL;
SELECT 
    stock_code,
    corp_name,
    industry
FROM TB_CORP_INFO 
WHERE industry IS NOT NULL
LIMIT 5;
"
```

**검증 포인트:**
- ✅ TB_CORP_INFO 테이블에 업종 정보 업데이트
- ✅ OpenDART API 연동 확인
- ✅ 업종 정보 존재 확인

### 2.2 재무 정보 수집 (DART API)

```bash
# DART API를 통한 재무 정보 수집
curl -X POST "http://localhost:8082/batch/corp-fin?date=20240213"

# 로그 모니터링 (DART API 호출 확인)
docker logs stock-finance -f | grep -E "(DART|Corp code|계정과목)"

# 결과 확인 (배치 완료까지 약 10-15분 소요, 100ms 딜레이)
sleep 900
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT COUNT(*) as total_finance FROM TB_CORP_FINANCE;
SELECT 
    validation_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM TB_CORP_FINANCE), 2) as percentage
FROM TB_CORP_FINANCE 
GROUP BY validation_status;
SELECT 
    corp_code, 
    biz_year, 
    report_code,
    total_asset, 
    total_debt, 
    total_capital, 
    revenue, 
    op_income, 
    net_income,
    operating_cashflow,
    free_cashflow,
    ebitda
FROM TB_CORP_FINANCE 
WHERE validation_status = 'VERIFIED' 
LIMIT 5;
"
```

**검증 포인트:**
- ✅ DART API 호출 성공 (Corp Code 매핑)
- ✅ 재무제표 정합성 검증 (자산 = 부채 + 자본)
- ✅ ValidationStatus 분포 확인 (VERIFIED, INVALID, WARNING)
- ✅ 필수 필드 존재 (자산총계, 부채총계, 자본총계, 매출액, 영업이익, 순이익)
- ✅ 현금흐름 데이터 수집 (operating_cashflow, investing_cashflow, financing_cashflow)
- ✅ 계산 지표 생성 (free_cashflow, ebitda)
- ✅ 90% 이상 VERIFIED 상태 (DART API 품질 향상)
- ✅ DataGo API fallback 동작 확인 (DART 실패 시)

### 2.2.1 재무 지표 계산 (부가 데이터)

```bash
# 재무 지표 자동 계산 확인
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT COUNT(*) as total_indicators FROM TB_CORP_FINANCE_INDICATOR;
SELECT 
    corp_code,
    bas_dt,
    report_code,
    per,
    pbr,
    psr,
    pcr,
    ev_ebitda,
    roe,
    roa,
    operating_margin,
    net_margin,
    fcf_yield,
    qoq_revenue_growth,
    yoy_revenue_growth
FROM TB_CORP_FINANCE_INDICATOR 
WHERE per IS NOT NULL 
LIMIT 5;

SELECT 
    COUNT(*) as total,
    COUNT(per) as per_count,
    COUNT(pbr) as pbr_count,
    COUNT(roe) as roe_count,
    COUNT(pcr) as pcr_count,
    COUNT(qoq_revenue_growth) as qoq_count,
    COUNT(yoy_revenue_growth) as yoy_count,
    ROUND(COUNT(per) * 100.0 / COUNT(*), 2) as success_rate
FROM TB_CORP_FINANCE_INDICATOR;
"
```

**검증 포인트:**
- ✅ TB_CORP_FINANCE_INDICATOR 테이블 데이터 생성
- ✅ 가치평가 지표: PER, PBR, PSR, PCR, EV/EBITDA
- ✅ 수익성 지표: ROE, ROA, Operating Margin, Net Margin, FCF Yield
- ✅ 성장률 지표: QoQ (3개), YoY (3개)
- ✅ 90% 이상 지표 계산 성공률
- ✅ 분기별 데이터 지원 (report_code: 11013, 11012, 11014, 11011)

### 2.3 주가 데이터 수집

```bash
# 주가 데이터 수집 (시장별)
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20240213"
sleep 180
curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20240213"
sleep 180
curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20240213"

# 결과 확인 (배치 완료까지 약 3-5분 소요)
sleep 180
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total_prices FROM TB_STOCK_PRICE;
SELECT stock_code, bas_dt, end_price, volume FROM TB_STOCK_PRICE 
WHERE bas_dt = '2024-02-13' LIMIT 5;
"
```

**검증 포인트:**
- ✅ Stock code 형식: `900100` (숫자만)
- ✅ 일별 주가 데이터 수집
- ✅ OHLCV 데이터 완전성 (start_price, high_price, low_price, end_price, volume)
- ✅ 거래량 > 0
- ✅ 시장별 수집 (KOSPI, KOSDAQ, KONEX)

### 2.4 수정주가 계산

```bash
# 수정주가 계산 배치 실행
curl -X POST "http://localhost:8083/batch/adjusted-price?date=20240213"

# 결과 확인 (배치 완료까지 약 5-10분 소요)
sleep 600
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    stock_code,
    bas_dt,
    end_price,
    adj_close_price,
    ROUND((adj_close_price / end_price), 4) as adjustment_ratio
FROM TB_STOCK_PRICE 
WHERE adj_close_price IS NOT NULL
  AND bas_dt = '2024-02-13'
LIMIT 10;
"
```

**검증 포인트:**
- ✅ `adj_close_price` 필드 계산 완료
- ✅ 액면분할/배당 이벤트 반영
- ✅ 조정 비율 합리성 확인 (0.5 ~ 2.0 범위)
- ✅ NULL 값 최소화

### 2.5 기술적 지표 계산

```bash
# 기술적 지표 계산 배치 실행
curl -X POST "http://localhost:8083/batch/indicators?date=20240213"

# 결과 확인 (배치 완료까지 약 10-15분 소요)
sleep 900
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total_indicators FROM TB_STOCK_INDICATOR;
SELECT 
    stock_code,
    indicator_date,
    rsi_14,
    macd,
    macd_signal,
    bollinger_upper,
    bollinger_middle,
    bollinger_lower,
    momentum_1m,
    momentum_3m,
    momentum_6m
FROM TB_STOCK_INDICATOR 
WHERE indicator_date = '2024-02-13'
LIMIT 5;

SELECT 
    COUNT(*) as total,
    COUNT(rsi_14) as rsi_count,
    COUNT(macd) as macd_count,
    COUNT(bollinger_upper) as bb_count,
    COUNT(momentum_1m) as momentum_count,
    ROUND(COUNT(rsi_14) * 100.0 / COUNT(*), 2) as indicator_coverage
FROM TB_STOCK_INDICATOR;
"
```

**검증 포인트:**
- ✅ RSI (14일) 계산
- ✅ MACD 및 Signal 계산
- ✅ Bollinger Bands (상단, 중간, 하단) 계산
- ✅ Momentum 지표 (1개월, 3개월, 6개월)
- ✅ 최소 300거래일 데이터 확보 검증
- ✅ 지표 값 범위 합리성 (RSI: 0-100)
- ✅ 지표 커버리지 90% 이상

### 2.6 재무 지표 계산 (삭제 - 2.2.1로 통합)

---

## 3단계: 서비스 간 통신 검증 (10분)

### 3.1 stock-finance → stock-price 통신

```bash
# 재무 지표 계산 시 주가 데이터 조회 로그 확인
docker logs stock-finance 2>&1 | grep -i "StockClient" | tail -20

# Stock code 형식 변환 확인
docker logs stock-finance 2>&1 | grep -E "(A[0-9]{6}|replace)" | tail -10
```

**검증 포인트:**
- ✅ Stock code 형식 변환 (`A900100` → `900100`)
- ✅ RestClient 정상 동작
- ✅ 타임아웃 설정 확인 (연결 5초, 읽기 10초)
- ✅ 에러 처리 로직 동작

### 3.2 stock-strategy → stock-corp/price/finance 통신

```bash
# 백테스팅 실행 시 로그 확인 (4단계에서 실행 후)
docker logs stock-strategy 2>&1 | grep -i "Client" | tail -30
docker logs stock-strategy 2>&1 | grep -E "(CorpClient|PriceClient|FinanceClient)" | tail -20
```

**검증 포인트:**
- ✅ 유니버스 필터링 시 기업 정보 조회
- ✅ 주가 데이터 조회
- ✅ 재무 지표 배치 조회 (N+1 문제 방지)
- ✅ 프로파일별 URL 설정 (local/prod)

### 3.3 서비스 디스커버리 확인

```bash
# Eureka에 등록된 서비스 확인
curl -s -u $EUREKA_USER:$EUREKA_PASSWORD http://localhost:8761/eureka/apps | grep -E "(application|status)" | head -20
```

**검증 포인트:**
- ✅ 모든 서비스 Eureka 등록
- ✅ 서비스 상태 UP
- ✅ 인스턴스 정보 정확성

---

## 배치 실행 순서 요약

**중요**: 데이터 수집은 반드시 다음 순서로 진행해야 합니다 (의존성 존재).

1. **기업 정보 수집** (필수, 선행 조건, DART 고유번호 자동 매핑)
   ```bash
   curl -X POST "http://localhost:8081/batch/corp-info?date=20240213"
   ```
   - 기업 기본 정보 수집
   - DART Corp Code 자동 다운로드 및 매핑

2. **기업 상세 정보 수집** (부가 데이터, 기업 정보 필요)
   ```bash
   curl -X POST "http://localhost:8081/batch/corp-detail/sector-update"
   ```

3. **주가 데이터 수집** (기업 정보 필요, 시장별 순차 실행)
   ```bash
   curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20240213"
   curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20240213"
   curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20240213"
   ```

5. **수정주가 계산** (주가 데이터 필요)
   ```bash
   curl -X POST "http://localhost:8083/batch/adjusted-price?date=20240213"
   ```

6. **기술적 지표 계산** (수정주가 필요)
   ```bash
   curl -X POST "http://localhost:8083/batch/indicators?date=20240213"
   ```

6. **재무 정보 수집** (기업 정보, 주가 데이터 필요, DART 고유번호 자동 사용)
   ```bash
   curl -X POST "http://localhost:8082/batch/corp-fin?date=20240213"
   ```
   - 재무 지표(TB_CORP_FINANCE_INDICATOR)는 자동 계산됨
   - DART Corp Code는 기업 정보 수집 시 자동 매핑됨

**총 소요 시간**: 약 40-50분

**수집되는 데이터 요약:**
- **주요 데이터**: TB_CORP_INFO, TB_STOCK_PRICE, TB_CORP_FINANCE
- **부가 데이터**: industry (TB_CORP_INFO), TB_STOCK_INDICATOR, TB_CORP_FINANCE_INDICATOR
- **계산 데이터**: adj_close_price, free_cashflow, ebitda, 재무 지표 16개

---

## 주의사항

1. **API 엔드포인트 경로**
   - 컨트롤러는 `/batch/*` 경로 사용
   - Gateway 라우팅 설정 확인 필요
   - 직접 호출 시: `http://localhost:808X/batch/*`
   - Gateway 통한 호출 시: `http://localhost:8080/api/v1/{service}/batch/*`

2. **배치 실행 시간**
   - 각 배치는 완료까지 수 분 소요
   - 동시 실행 시 리소스 경합 가능
   - 순차 실행 권장

3. **데이터 의존성**
   - 기업 정보 → 주가 데이터 → 수정주가 → 기술적 지표
   - 재무 정보는 기업 정보 + 주가 데이터 필요

4. **Stock Code 형식**
   - 기업 정보: `A900100` (A 접두사)
   - 주가 데이터: `900100` (숫자만)
   - 서비스 간 통신 시 형식 변환 필수

5. **DART API 사용량 제한 (중요)**
   - **일일 호출 한도**: 10,000건/일
   - **리셋 시간**: 매일 자정 (KST)
   - **에러 코드**: 020 - 사용한도를 초과하였습니다
   - **영향**: 재무 데이터 수집 실패 시 전체 검증 불가
   - **권장 사항**:
     - 검증은 하루 1회만 실행
     - 개발/테스트 시 소량 데이터로 테스트
     - 한도 초과 시 다음날 재시도
     - DataGo API fallback 동작 확인 필요
   - **계산식**: 2,700개 기업 × 4개 분기 = 10,800건 (한도 초과 가능)

6. **재무제표 공시 일정 (중요)**
   - **공시 시점**: 결산 후 1.5~3개월 소요
   - **미래 연도 조회 불가**: 아직 공시되지 않은 데이터는 "013 - 조회된 데이타가 없습니다" 반환
   - **권장 테스트 날짜**:
     - **전체 검증**: 2024-10-14 (2024년 모든 분기 공시 완료)
     - **최신 데이터**: 2025-09-30 (2025년 Q1, 반기 공시 완료)
     - **부분 검증**: 2023-12-31 (2023년 전체 데이터 안정적)
   - **공시 일정표**:
     | 보고서 | 결산 기준일 | 공시 시점 |
     |--------|------------|----------|
     | 1분기 (Q1) | 3월 31일 | 5월 중순 |
     | 반기 (SEMI) | 6월 30일 | 8월 중순 |
     | 3분기 (Q3) | 9월 30일 | 11월 중순 |
     | 사업보고서 (ANNUAL) | 12월 31일 | 익년 3월 |

---

## 4단계: 백테스팅 엔진 검증 (Phase 2) (30분)

### 4.1 Equal Weight 전략 백테스팅

```bash
# 백테스팅 요청
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "EQUAL_WEIGHT",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "commissionRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI",
      "minMarketCap": 1000000000,
      "minVolume": 100000
    }
  }'

# 응답에서 simulationId 확인 (예: 1)

# 결과 조회
curl http://localhost:8084/backtest/1/result | jq .
```

**검증 포인트:**
- ✅ 시뮬레이션 상태: `COMPLETED`
- ✅ CAGR, MDD, Sharpe Ratio 계산
- ✅ 거래 이력 저장
- ✅ 포트폴리오 스냅샷 저장
- ✅ 최종 자산 가치 > 0

### 4.2 Momentum 전략 백테스팅

```bash
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "MOMENTUM",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "commissionRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI",
      "minMarketCap": 1000000000
    }
  }'
```

**검증 포인트:**
- ✅ 모멘텀 지표 활용 확인
- ✅ 상위 20개 종목 선정
- ✅ 리밸런싱 로직 정상 동작
- ✅ 성과 지표 계산

### 4.3 Low Volatility 전략 백테스팅

```bash
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "LOW_VOLATILITY",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "commissionRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI"
    }
  }'
```

**검증 포인트:**
- ✅ 60일 변동성 계산
- ✅ 변동성 하위 20개 종목 선정
- ✅ 안정적 수익 추구 확인
- ✅ MDD가 다른 전략 대비 낮음

### 4.4 Value 전략 백테스팅 (가중치 커스터마이징)

```bash
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "VALUE",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "commissionRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI",
      "minMarketCap": 1000000000
    },
    "valueStrategyConfig": {
      "topN": 30,
      "perWeight": 0.5,
      "pbrWeight": 0.3,
      "roeWeight": 0.2
    }
  }'
```

**검증 포인트:**
- ✅ 재무 지표 배치 조회 (FinanceClient)
- ✅ 가치 스코어 계산 (PER, PBR, ROE)
- ✅ 가중치 커스터마이징 반영
- ✅ 상위 N개 종목 선정
- ✅ 재무 지표 누락 종목 제외 처리

### 4.5 백테스팅 결과 상세 조회

```bash
# 포트폴리오 스냅샷 조회
curl http://localhost:8084/backtest/1/snapshots | jq .

# 거래 이력 조회
docker exec stock-strategy-db mysql -u strategy_user -pstrategy_pass stock_strategy -e "
SELECT 
    trade_date,
    stock_code,
    order_type,
    quantity,
    price,
    commission,
    tax
FROM TB_TRADE_HISTORY 
WHERE simulation_id = 1
ORDER BY trade_date
LIMIT 20;
"

# 성과 지표 조회
docker exec stock-strategy-db mysql -u strategy_user -pstrategy_pass stock_strategy -e "
SELECT 
    simulation_id,
    cagr,
    mdd,
    sharpe_ratio,
    volatility,
    win_rate,
    total_trades
FROM TB_BACKTEST_RESULT
WHERE simulation_id = 1;
"
```

**검증 포인트:**
- ✅ 리밸런싱일 스냅샷 저장
- ✅ 매매 수수료 및 세금 계산
- ✅ 보유 종목 JSON 저장
- ✅ 일별 포트폴리오 가치 추적

---

## 5단계: 입력값 검증 테스트 (10분)

### 5.1 필수 필드 누락 테스트

```bash
# 잘못된 요청 (startDate 누락)
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "EQUAL_WEIGHT",
    "endDate": "2025-12-31",
    "initialCapital": 10000000
  }'

# 예상 응답: 400 Bad Request
# {
#   "timestamp": "...",
#   "status": 400,
#   "error": "Validation Failed",
#   "message": "입력값 검증에 실패했습니다",
#   "details": {
#     "startDate": "백테스팅 시작 날짜는 필수입니다"
#   }
# }
```

**검증 포인트:**
- ✅ HTTP 400 응답
- ✅ 명확한 에러 메시지 (한글)
- ✅ 누락된 필드 명시
- ✅ GlobalExceptionHandler 동작

### 5.2 날짜 형식 검증 테스트

```bash
# 잘못된 날짜 형식
curl -X POST "http://localhost:8083/batch/stock-price?date=2026-02-14"

# 예상 응답: 400 Bad Request
# "날짜 형식은 yyyyMMdd 형식이어야 합니다"
```

**검증 포인트:**
- ✅ Pattern 검증 동작
- ✅ 정규식 매칭 실패 시 에러
- ✅ @Validated 어노테이션 동작

### 5.3 금액 범위 검증 테스트

```bash
# 초기 자본금 0 이하
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "EQUAL_WEIGHT",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": -1000000
  }'

# 예상 응답: 400 Bad Request
# "초기 자본금은 0보다 커야 합니다"
```

**검증 포인트:**
- ✅ DecimalMin 검증 동작
- ✅ 음수 값 거부
- ✅ 명확한 에러 메시지

### 5.4 중첩 객체 검증 테스트

```bash
# 가중치 합계 검증 (VALUE 전략)
curl -X POST http://localhost:8084/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "VALUE",
    "startDate": "2023-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 10000000,
    "valueStrategyConfig": {
      "topN": 30,
      "perWeight": 0.5,
      "pbrWeight": 0.5,
      "roeWeight": 0.5
    }
  }'

# 예상 응답: 400 Bad Request
# "가중치 합계는 1.0이어야 합니다"
```

**검증 포인트:**
- ✅ @Valid 중첩 검증 동작
- ✅ 커스텀 검증 로직 실행
- ✅ 비즈니스 규칙 검증

---

## 6단계: 데이터 정합성 검증 (10분)

### 6.1 재무제표 대차대조표 등식 검증

```bash
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT 
    corp_code,
    total_assets,
    total_liabilities,
    total_equity,
    ABS(total_assets - (total_liabilities + total_equity)) as diff,
    validation_status
FROM TB_CORP_FINANCE 
WHERE validation_status = 'VALID'
LIMIT 10;

SELECT 
    validation_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM TB_CORP_FINANCE), 2) as percentage
FROM TB_CORP_FINANCE 
GROUP BY validation_status;
"
```

**검증 포인트:**
- ✅ `diff` 값이 허용 오차 범위 내 (< 1000)
- ✅ `validation_status = 'VALID'` 비율 95% 이상
- ✅ 자산 = 부채 + 자본 등식 성립
- ✅ 필수 필드 NULL 아님

### 6.2 수정주가 합리성 검증

```bash
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    stock_code,
    bas_dt,
    end_price,
    adj_close_price,
    ROUND((adj_close_price / end_price), 4) as adjustment_ratio
FROM TB_STOCK_PRICE 
WHERE adj_close_price IS NOT NULL
  AND ABS(adj_close_price / end_price - 1) > 0.01
ORDER BY ABS(adj_close_price / end_price - 1) DESC
LIMIT 10;

SELECT 
    COUNT(*) as total,
    COUNT(adj_close_price) as adjusted_count,
    ROUND(COUNT(adj_close_price) * 100.0 / COUNT(*), 2) as coverage
FROM TB_STOCK_PRICE;
"
```

**검증 포인트:**
- ✅ 조정 비율이 합리적 범위 내 (0.5 ~ 2.0)
- ✅ 액면분할 이벤트 반영 확인
- ✅ 수정주가 커버리지 확인
- ✅ 극단적 조정 비율 원인 분석

### 6.3 기술적 지표 범위 검증

```bash
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    stock_code,
    indicator_date,
    rsi_14,
    CASE 
        WHEN rsi_14 < 0 OR rsi_14 > 100 THEN 'INVALID'
        ELSE 'VALID'
    END as rsi_status,
    macd,
    bollinger_upper,
    bollinger_lower
FROM TB_STOCK_INDICATOR 
WHERE rsi_14 IS NOT NULL
ORDER BY indicator_date DESC
LIMIT 10;

SELECT 
    COUNT(*) as total,
    COUNT(rsi_14) as rsi_count,
    COUNT(macd) as macd_count,
    COUNT(momentum_1m) as momentum_count,
    ROUND(COUNT(rsi_14) * 100.0 / COUNT(*), 2) as indicator_coverage
FROM TB_STOCK_INDICATOR;
"
```

**검증 포인트:**
- ✅ RSI 값이 0~100 범위 내
- ✅ MACD, Bollinger Bands 값 합리성
- ✅ Momentum 지표 계산 완료
- ✅ 지표 커버리지 확인

### 6.4 재무 지표 이상치 검증

```bash
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT 
    corp_code,
    per,
    pbr,
    roe,
    roa,
    CASE 
        WHEN per < 0 THEN 'NEGATIVE_PER'
        WHEN per > 100 THEN 'HIGH_PER'
        WHEN pbr < 0 THEN 'NEGATIVE_PBR'
        WHEN pbr > 10 THEN 'HIGH_PBR'
        ELSE 'NORMAL'
    END as status
FROM TB_CORP_FINANCE 
WHERE per IS NOT NULL
  AND (per < 0 OR per > 100 OR pbr < 0 OR pbr > 10)
LIMIT 10;
"
```

**검증 포인트:**
- ✅ 음수 PER 원인 분석 (적자 기업)
- ✅ 극단적 PBR 값 확인
- ✅ ROE, ROA 범위 합리성
- ✅ 이상치 비율 확인

### 6.5 Stock Code 형식 일관성 검증

```bash
# 기업 정보 Stock Code 형식
docker exec stock-corp-db mysql -u corp_user -pcorp_pass stock_corp -e "
SELECT 
    stock_code,
    CASE 
        WHEN stock_code LIKE 'A%' THEN 'VALID'
        ELSE 'INVALID'
    END as format_status
FROM TB_CORP_INFO 
WHERE stock_code NOT LIKE 'A%'
LIMIT 5;
"

# 주가 데이터 Stock Code 형식
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    stock_code,
    CASE 
        WHEN stock_code REGEXP '^[0-9]{6}$' THEN 'VALID'
        ELSE 'INVALID'
    END as format_status
FROM TB_STOCK_PRICE 
WHERE stock_code NOT REGEXP '^[0-9]{6}$'
LIMIT 5;
"
```

**검증 포인트:**
- ✅ 기업 정보: `A` 접두사 일관성
- ✅ 주가 데이터: 숫자 6자리 일관성
- ✅ 형식 불일치 건수 확인
- ✅ 서비스 간 변환 로직 검증

---

## 검증 체크리스트

### Phase 0: 인프라 구축
- [ ] 모든 서비스 정상 기동
- [ ] Eureka 서비스 등록 확인
- [ ] Gateway 라우팅 정상 동작
- [ ] 데이터베이스 연결 정상

### Phase 1: 데이터 무결성
- [ ] 기업 정보 수집 (2,000개 이상)
- [ ] 재무제표 정합성 검증 (95% 이상)
- [ ] 주가 데이터 수집 (OHLCV 완전성)
- [ ] 수정주가 계산 (조정 비율 합리성)
- [ ] 기술적 지표 계산 (RSI, MACD, Bollinger, Momentum)
- [ ] 재무 지표 계산 (PER, PBR, ROE, ROA, 93% 이상)

### Phase 2: 백테스팅 엔진
- [ ] Equal Weight 전략 정상 동작
- [ ] Momentum 전략 정상 동작
- [ ] Low Volatility 전략 정상 동작
- [ ] Value 전략 정상 동작 (가중치 커스터마이징)
- [ ] 성과 지표 계산 (CAGR, MDD, Sharpe Ratio)
- [ ] 거래 이력 저장
- [ ] 포트폴리오 스냅샷 저장
- [ ] 입력값 검증 시스템 동작

### 서비스 간 통신
- [ ] stock-finance → stock-price 통신
- [ ] stock-strategy → stock-corp 통신
- [ ] stock-strategy → stock-price 통신
- [ ] stock-strategy → stock-finance 통신
- [ ] Stock code 형식 변환 정상 동작

### 데이터 정합성
- [ ] 재무제표 대차대조표 등식 검증
- [ ] 수정주가 합리성 검증
- [ ] 기술적 지표 범위 검증
- [ ] 재무 지표 이상치 분석
- [ ] Stock code 형식 일관성

---

## 문제 발생 시 대응

### 1. 배치 실행 실패
```bash
# 배치 메타데이터 확인
docker exec stock-batch-db mysql -u batch_user -pbatch_pass stock_batch -e "
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY job_execution_id DESC LIMIT 5;
"

# 에러 로그 확인
docker logs [service-name] 2>&1 | grep -E "(ERROR|Exception)" | tail -50
```

### 2. 서비스 간 통신 실패
```bash
# 네트워크 연결 확인
docker exec stock-finance curl -s http://stock-price:8083/

# 서비스 디스커버리 확인
curl -s -u $EUREKA_USER:$EUREKA_PASSWORD http://localhost:8761/eureka/apps
```

### 3. 데이터베이스 연결 실패
```bash
# 컨테이너 재시작
docker-compose restart [db-service-name]

# 로그 확인
docker logs [db-service-name] 2>&1 | tail -50
```

### 4. 메모리 부족
```bash
# 컨테이너 리소스 사용량 확인
docker stats

# 불필요한 컨테이너 정리
docker system prune -a
```

---

## 다음 단계

검증 완료 후:
1. **Phase 3 준비**: 종목 추천 시스템 설계
2. **성능 최적화**: 배치 처리 속도 개선
3. **모니터링 구축**: Prometheus + Grafana
4. **문서화**: API 문서 업데이트

---

**작성일**: 2026-02-15  
**버전**: 1.0  
**상태**: Phase 0-2 검증용
