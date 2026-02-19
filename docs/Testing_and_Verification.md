# 테스트 및 검증 가이드

> **목적**: Phase 0-2 시스템의 데이터 수집, 처리, 백테스팅 기능 검증
> 
> **최종 업데이트**: 2026-02-16
> - API 엔드포인트 실제 구현 반영
> - DART API HTTPS 이슈 해결
> - Gateway Netty/Undertow 충돌 해결

## 📋 목차
- [검증 계획](#검증-계획)
- [테스트 가이드](#테스트-가이드)
- [발견된 이슈](#발견된-이슈)

---

## 검증 계획

### Phase 0: 데이터 수집 검증
**목표**: 외부 API 연동 및 데이터 수집 안정성 확인

#### 1. 기업 정보 수집
- [ ] 전체 상장 기업 수집 (2,700+ 예상)
- [ ] DART Corp Code 자동 매핑 (90%+ 목표)
- [ ] 기업 상세 정보 (업종 분류)

#### 2. 주가 데이터 수집
- [ ] KOSPI 시장 (700+ 종목)
- [ ] KOSDAQ 시장 (1,500+ 종목)
- [ ] KONEX 시장 (100+ 종목)
- [ ] 수정주가 계산 (액면분할/배당 조정)

#### 3. 재무 데이터 수집 (DART API)
- [ ] 4개 분기 데이터 수집 (Q1, SEMI, Q3, ANNUAL)
- [ ] 재무상태표 (자산, 부채, 자본)
- [ ] 손익계산서 (매출, 영업이익, 순이익)
- [ ] 현금흐름표 (영업CF, 투자CF, 재무CF)
- [ ] 계산 지표 (FCF, EBITDA)

#### 4. 기술적 지표 계산
- [ ] RSI (14일)
- [ ] MACD (12, 26, 9)
- [ ] Bollinger Bands (20일, 2σ)
- [ ] Momentum (10일)

#### 5. 재무 지표 계산
- [ ] 가치평가: PER, PBR, PSR, PCR, EV/EBITDA, FCF Yield
- [ ] 수익성: ROE, ROA, Operating Margin, Net Margin
- [ ] 성장률: QoQ, YoY (매출, 영업이익, 순이익)

### Phase 1: 데이터 품질 검증
**목표**: 수집된 데이터의 정합성 및 완전성 확인

#### 1. 재무제표 검증
- [ ] 대차대조표 등식: 자산 = 부채 + 자본
- [ ] 필수 필드 존재 여부
- [ ] 음수 값 검증 (자산, 부채, 자본)
- [ ] 이상치 탐지 (극단적 비율)

#### 2. 데이터 완전성
- [ ] 기업 정보 커버리지 (90%+ 목표)
- [ ] 주가 데이터 누락 확인
- [ ] 재무 데이터 수집률 (80%+ 목표)
- [ ] 지표 계산 성공률 (90%+ 목표)

#### 3. 성능 검증
- [ ] 배치 처리 시간 (전체 < 30분)
- [ ] API 호출 제한 준수 (DART 10,000/day)
- [ ] 메모리 사용량 모니터링
- [ ] 데이터베이스 쿼리 성능

### Phase 2: 백테스팅 검증
**목표**: 전략 실행 및 결과 정확성 확인

#### 1. 전략 실행
- [ ] Momentum 전략 (상위 20% 선택)
- [ ] Value 전략 (PER, PBR 기반)
- [ ] Quality 전략 (ROE, 부채비율 기반)
- [ ] 리밸런싱 주기 (월간, 분기, 반기, 연간)

#### 2. 결과 검증
- [ ] 수익률 계산 정확성
- [ ] 거래 내역 추적
- [ ] 포트폴리오 구성 변화
- [ ] 벤치마크 대비 성과

---

## 테스트 가이드

### ⚠️ 중요: 배치 실행 필수

**이 섹션의 모든 배치는 반드시 순서대로 실행해야 합니다.**
- 각 배치는 이전 단계의 데이터에 의존합니다
- 순서를 지키지 않으면 데이터 무결성 문제가 발생합니다
- 전체 소요 시간: 약 20-30분

### 환경 준비

#### 1. Docker 환경 초기화
```bash
# 기존 데이터 삭제 및 재시작
cd /Users/kafa/IdeaProjects/stock-msa
docker-compose down -v
docker-compose up -d --build

# 서비스 시작 대기 (3분)
sleep 180
```

#### 2. 서비스 상태 확인
```bash
# 컨테이너 상태
docker ps

# 데이터베이스 연결 확인
docker exec stock-corp-db mysql -u corp_user -pcorp_pass -e "SELECT 1"
docker exec stock-finance-db mysql -u finance_user -pfinance_pass -e "SELECT 1"
docker exec stock-price-db mysql -u stock_user -pstock_pass -e "SELECT 1"
```

### 배치 실행 순서

**⚠️ 필수 실행 항목**: 아래 모든 배치를 순서대로 실행해야 합니다.

**⚠️ 중요**: 2024년 데이터 사용 (2024-10-14 권장)
- DART API는 재무제표 발표 후 1.5-3개월 후 데이터 제공
- 2025년 데이터는 아직 미발표 상태

#### 1. 기업 정보 수집 (DART Corp Code 자동 매핑) - 필수 ✅
```bash
curl -X POST "http://localhost:8081/batch/corp-info?date=20241014"
sleep 180

# 결과 확인
docker exec stock-corp-db mysql -u corp_user -pcorp_pass stock_corp -e "
SELECT 
    COUNT(*) as total,
    COUNT(dart_corp_code) as with_dart_code,
    ROUND(COUNT(dart_corp_code) * 100.0 / COUNT(*), 2) as mapping_rate
FROM TB_CORP_INFO;"
```

**예상 결과**: 2,700+ 기업, DART 매핑률 90%+

#### 2. 기업 상세 정보 (업종) - 필수 ✅

**⚠️ 주의**: 문서 작성 시점에 `/batch/corp-detail` 엔드포인트가 계획되었으나, 실제 구현은 두 개의 독립 엔드포인트로 분리되었습니다.

```bash
# 업종 정보 수집 (DART API)
curl -X POST "http://localhost:8081/batch/corp-detail/sector-update"
sleep 60

# (선택) 기업 상태 정리 - 최신 정보로 갱신되지 않은 기업 DEL 처리
curl -X POST "http://localhost:8081/batch/corp-detail/cleanup"
```

**참고**: 업종 정보 수집은 DART API를 사용하며, 현재 HTTP → HTTPS 변경으로 안정성이 개선되었습니다.

#### 3. 주가 데이터 수집 (수정주가 및 기술적 지표 자동 계산) - 필수 ✅
주가 데이터 수집 배치는 다음 작업을 순차적으로 자동 실행합니다:
1. **주가 데이터 수집**: 시가, 고가, 저가, 종가, 거래량
2. **기업 이벤트 수집**: 액면분할, 배당 등
3. **수정주가 계산**: 이벤트 기반 조정
4. **기술적 지표 계산**: RSI, MACD, Bollinger Bands, Momentum

```bash
# KOSPI (약 3-5분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20241014"
sleep 300

# KOSDAQ (약 3-5분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20241014"
sleep 300

# KONEX (약 1분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20241014"
sleep 60

# 결과 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total_prices FROM TB_STOCK_PRICE WHERE bas_dt = '2024-10-14';"

# 기술적 지표 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    COUNT(*) as total,
    COUNT(rsi) as rsi_count,
    COUNT(macd) as macd_count,
    COUNT(bb_upper) as bb_count,
    COUNT(momentum) as momentum_count
FROM TB_STOCK_PRICE 
WHERE bas_dt = '2024-10-14';"
```

**예상 결과**: 
- 주가 데이터: 2,300+ 건
- 기술적 지표: 2,000+ 건 (300일 이상 히스토리 필요)

#### 4. 재무 데이터 수집 (DART API)

#### 3. 주가 데이터 수집 (3개 시장)
주가 데이터 수집 시 다음 작업이 자동으로 순차 실행됩니다:
1. 주가 데이터 수집
2. 기업 이벤트 수집 (액면분할, 배당 등)
3. 수정주가 계산
4. 기술적 지표 계산 (RSI, MACD, Bollinger Bands, Momentum)

```bash
# KOSPI (약 3-5분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20241014"
sleep 300

# KOSDAQ (약 3-5분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20241014"
sleep 300

# KONEX (약 1분 소요)
curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20241014"
sleep 60

# 결과 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total_prices FROM TB_STOCK_PRICE WHERE bas_dt = '2024-10-14';"

# 기술적 지표 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    COUNT(*) as total,
    COUNT(rsi) as rsi_count,
    COUNT(macd) as macd_count,
    COUNT(bb_upper) as bb_count,
    COUNT(momentum) as momentum_count
FROM TB_STOCK_PRICE 
WHERE bas_dt = '2024-10-14';"
```

**예상 결과**: 
- 주가 데이터: 2,300+ 건
- 기술적 지표: 2,000+ 건 (300일 이상 데이터 필요)

#### 4. 재무 데이터 수집 (DART API) - 필수 ✅

**⚠️ 중요**: 전체 배치 실행 전 단일 기업 테스트를 먼저 수행하세요.

##### 4-1. 단일 기업 테스트 (필수 ✅)
재무 데이터 수집 로직을 먼저 검증합니다.

```bash
# 삼성전자 2024년 재무제표 테스트
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A005930&year=2024"
```

**특징**:
- API 호출 4건 (Q1, SEMI, Q3, ANNUAL)
- 응답 시간 < 1초
- 상세 로그 출력
- JSON 응답으로 데이터 확인

**응답 예시**:
```json
{
  "stockCode": "A005930",
  "corpCode": "00126380",
  "year": 2024,
  "quarters": ["Q1", "SEMI", "Q3", "ANNUAL"],
  "collected": 4,
  "verified": 4,
  "indicators_calculated": true
}
```

**검증 항목**:
- ✅ DART Corp Code 매핑 성공
- ✅ 4개 분기 데이터 수집 완료
- ✅ 재무제표 검증 통과 (VERIFIED)
- ✅ 재무 지표 계산 완료

**추가 테스트 종목**:
```bash
# SK하이닉스
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A000660&year=2024"

# NAVER
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A035420&year=2024"
```

##### 4-2. 전체 데이터 수집 (단일 테스트 성공 후 필수 ✅)
단일 기업 테스트가 성공하면 전체 배치를 실행합니다.

```bash
curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014"
sleep 300

# 결과 확인
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT 
    report_code,
    COUNT(*) as count,
    COUNT(CASE WHEN validation_status = 'VERIFIED' THEN 1 END) as verified
FROM TB_CORP_FINANCE 
WHERE YEAR(bas_dt) = 2024
GROUP BY report_code;"
```

**예상 결과**: 
- Q1 (11013): 2,000+ 건
- SEMI (11012): 2,000+ 건
- Q3 (11014): 2,000+ 건
- ANNUAL (11011): 2,000+ 건
- VERIFIED 비율: 90%+

---

## 배치 실행 요약

**⚠️ 필수**: 전체 배치를 순서대로 실행해야 합니다 (총 소요 시간: 약 20-30분)

전체 배치 실행 순서:

```bash
# 1. 기업 정보 (3분)
curl -X POST "http://localhost:8081/batch/corp-info?date=20241014"
sleep 180

# 2. 업종 정보 (1분) - DART API 사용
curl -X POST "http://localhost:8081/batch/corp-detail/sector-update"
sleep 60

# 3. 주가 데이터 - KOSPI (5분, 수정주가+지표 자동 계산)
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20241014"
sleep 300

# 4. 주가 데이터 - KOSDAQ (5분, 수정주가+지표 자동 계산)
curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20241014"
sleep 300

# 5. 주가 데이터 - KONEX (1분, 수정주가+지표 자동 계산)
curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20241014"
sleep 60

# 6. 재무 데이터 - 단일 테스트 (필수, 1초 미만)
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A005930&year=2024"

# 7. 재무 데이터 - 전체 수집 (5분)
curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014"
sleep 300
```

**⚠️ 주의사항**:
- **모든 배치는 반드시 순서대로 실행해야 합니다**
- 주가 데이터 수집 시 수정주가 계산과 기술적 지표 계산이 자동으로 실행됩니다
- 별도의 API 호출이 필요하지 않습니다
- 각 시장별로 순차 실행을 권장합니다 (동시 실행 시 DB 부하 증가)
- 재무 데이터는 반드시 단일 테스트 성공 후 전체 수집을 실행하세요
- 배치 실행 중 로그를 모니터링하세요: `docker logs -f [service-name]`


### 데이터 검증 쿼리

#### 1. 기업 정보 검증
```sql
-- DART Corp Code 매핑률
SELECT 
    COUNT(*) as total,
    COUNT(dart_corp_code) as mapped,
    ROUND(COUNT(dart_corp_code) * 100.0 / COUNT(*), 2) as rate
FROM TB_CORP_INFO;

-- 시장별 분포
SELECT market, COUNT(*) as count 
FROM TB_CORP_INFO 
GROUP BY market;
```

#### 2. 재무 데이터 검증
```sql
-- 검증 상태별 집계
SELECT 
    validation_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM TB_CORP_FINANCE), 2) as percentage
FROM TB_CORP_FINANCE
GROUP BY validation_status;

-- 대차대조표 등식 위반 확인
SELECT corp_code, bas_dt, report_code,
    total_asset, total_debt, total_capital,
    (total_asset - total_debt - total_capital) as diff
FROM TB_CORP_FINANCE
WHERE ABS(total_asset - total_debt - total_capital) > 1000000
LIMIT 10;
```

#### 3. 재무 지표 검증
```sql
-- 지표 계산 성공률
SELECT 
    COUNT(*) as total,
    COUNT(per) as per_count,
    COUNT(pbr) as pbr_count,
    COUNT(roe) as roe_count,
    COUNT(roa) as roa_count
FROM TB_CORP_FINANCE_INDICATOR;

-- 이상치 탐지 (PER > 100 또는 < 0)
SELECT corp_code, bas_dt, per, pbr, roe
FROM TB_CORP_FINANCE_INDICATOR
WHERE per > 100 OR per < 0
LIMIT 10;
```

#### 4. 기술적 지표 검증
```sql
-- 지표별 계산 성공률
SELECT 
    COUNT(*) as total,
    COUNT(rsi) as rsi_count,
    COUNT(macd) as macd_count,
    COUNT(bb_upper) as bb_count,
    COUNT(momentum) as momentum_count
FROM TB_STOCK_INDICATOR
WHERE bas_dt = '2024-10-14';
```

---

## 발견된 이슈

### 0. 인프라 및 설정 이슈 (2026-02-16 수정 완료)

#### 이슈: DART API 연결 실패 (stock-corp)
**증상**:
```
I/O error on GET request for "http://opendart.fss.or.kr/api/company.json": 
HTTP/1.1 header parser received no bytes
```

**원인**:
- DART API는 HTTPS만 지원하는데 HTTP로 호출
- `DartClient.java`에서 `.scheme("http")` 사용

**해결**: ✅ 완료
```java
// Before
.scheme("http")

// After
.scheme("https")
```

**파일**: `services/stock-corp/src/main/java/com/stock/corp/client/DartClient.java`

#### 이슈: Gateway 시작 실패
**증상**:
```
BeanDefinitionOverrideException: Invalid bean definition with name 'conversionServicePostProcessor'
Cannot register bean definition for bean 'conversionServicePostProcessor' 
since there is already [WebFluxSecurityConfiguration] bound.
```

**원인**:
- Spring Cloud Gateway는 Reactive(Netty) 기반
- `spring-boot-starter-undertow` 추가로 Servlet 기반 빈 충돌

**해결**: ✅ 완료
```gradle
// Before
implementation 'org.springframework.boot:spring-boot-starter-undertow'

// After
// 제거 (Gateway는 Netty 사용)
```

**파일**: `services/stock-gateway/build.gradle`

**결과**:
- ✅ 모든 서비스 정상 시작
- ✅ DART API 호출 성공
- ✅ Gateway 라우팅 정상 동작

---

### 1. DART API 제약사항

#### 이슈: 2025년 데이터 조회 실패
**증상**:
```
DART API Error: 013 - 조회된 데이타가 없습니다
```

**원인**:
- 재무제표는 회계기간 종료 후 1.5-3개월 후 발표
- 2025년 Q1 (3월 31일 마감) → 5월 중순 발표
- 2025년 SEMI (6월 30일 마감) → 8월 중순 발표

**해결책**:
- ✅ 2024년 데이터 사용 (모든 분기 발표 완료)
- ✅ 테스트 날짜: 2024-10-14

#### 이슈: API 사용량 제한
**제약**:
- 일일 10,000건 제한
- 자정(KST) 초기화

**에러 코드**:
```
020 - 일일 사용량을 초과하였습니다
```

**대응**:
- API 호출 간 100ms 딜레이
- 배치 실행 시간 분산
- 실패 시 다음날 재시도

### 2. 데이터 품질 이슈

#### 이슈: 재무제표 검증 실패
**원인**:
- 대차대조표 등식 위반: 자산 ≠ 부채 + 자본
- 필수 필드 누락 (매출액, 순이익 등)
- 음수 값 (자산, 자본)

**현황**:
- VERIFIED: 90%+ (목표)
- FAILED: 5-10% (허용 범위)
- MISSING_DATA: < 5%

**대응**:
- ✅ 검증 로직 구현 (`ValidationStatus` enum)
- ✅ 실패 데이터 별도 표시
- ⏳ 수동 검토 프로세스 (Phase 1)

#### 이슈: Stock Code 형식 불일치
**문제**:
- 기업 정보: `A005930` (A 접두사)
- 주가 데이터: `005930` (숫자만)

**해결**:
```java
String stockCode = corpInfo.getStockCode().replace("A", "");
StockPriceDto stockPrice = stockClient.getLatestStockPrice(stockCode);
```

### 3. 성능 이슈

#### 이슈: 재무 지표 계산 실패
**원인**:
- 주가 데이터 누락 (시가총액 계산 불가)
- 재무 데이터 불완전 (분모 0)

**현황**:
- 계산 성공률: 93%
- 실패 원인: 주가 미수집 (7%)

**대응**:
- ✅ Null 체크 강화
- ✅ 0으로 나누기 방지
- ⏳ 주가 데이터 보완 (Phase 1)

---

## 체크리스트

### 배치 실행 전 (필수 확인)
- [ ] Docker 환경 정상 동작
- [ ] 데이터베이스 연결 확인
- [ ] API 키 설정 확인 (.env)
- [ ] 테스트 날짜 확인 (2024년 데이터)
- [ ] **배치 실행 순서 숙지** (1→2→3→4 순서 필수)

### 배치 실행 중 (필수 모니터링)
- [ ] 로그 모니터링 (`docker logs -f [service]`)
- [ ] API 에러 확인 (013, 020)
- [ ] 메모리 사용량 확인
- [ ] 처리 시간 기록

### 배치 실행 후 (필수 검증)
- [ ] **데이터 수집 건수 확인** (각 단계별 예상 결과와 비교)
- [ ] **검증 상태 확인** (VERIFIED 비율 90%+ 목표)
- [ ] **지표 계산 성공률 확인** (90%+ 목표)
- [ ] 이상치 데이터 검토

---

## 참고 자료

### API 문서
- [공공데이터포털](https://www.data.go.kr/)
- [DART API](https://opendart.fss.or.kr/guide/main.do)

### 내부 문서
- [구현 현황](./Implementation_Status.md)
- [구현 계획](./Implementation_Roadmap.md)

### 관련 파일
- `CorpFinanceBatch.java`: 재무 데이터 수집 배치
- `DartClient.java`: DART API 클라이언트
- `CorpFinanceService.java`: 재무 데이터 처리 서비스
