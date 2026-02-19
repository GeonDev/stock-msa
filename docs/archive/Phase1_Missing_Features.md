# Phase 1 누락 기능 구현 계획

## 문서 개요

**작성일**: 2026-02-15  
**최종 업데이트**: 2026-02-15  
**목적**: Phase 1 (데이터 무결성 및 전처리) 완료를 위한 누락 기능 구현  
**현재 상태**: Phase 1 진행 중 - DART API 전환 작업 중

---

## 현재 상태 분석

### ✅ 구현 완료
1. **기업 정보 수집**: 2,750개 기업 (KOSPI, KOSDAQ, KONEX)
2. **재무 정보 수집**: 2,579개 (2024년 기준, DataGo API)
3. **주가 데이터 수집**: 2,663개 종목 (2024-02-13)
4. **BigDecimal 정밀도**: 모든 금융 데이터 DECIMAL(25, 4)
5. **DART API 클라이언트**: DartClient, DartFinanceConverter 구현 완료
6. **Corp Code 매핑**: XML 파싱 및 캐싱 시스템 구현

### 🔄 진행 중
1. **DART API 전환**: DataGo → DART API 마이그레이션 (빌드 완료, 테스트 대기)
2. **재무 지표 계산**: 주가 데이터 연동 (CorpClient 추가 완료)

### ❌ 미구현 항목
1. **수정주가 계산 배치 API**
2. **기술적 지표 계산 배치 API**

---

## 누락 기능 상세

### 1. 수정주가 (Adjusted Price) 계산 배치

#### 현재 상태
- ❌ 배치 API 없음 (`/batch/adjusted-price`)
- ❌ `AdjustedPriceService` 미구현
- ❌ `CorpEventHistory` 엔티티만 존재

#### 필요한 구현
```
services/stock-price/
├── controller/StockController.java
│   └── POST /batch/adjusted-price (신규)
├── service/AdjustedPriceService.java (신규)
├── batchJob/
│   ├── AdjustedPriceBatch.java (신규)
│   └── ItemReader/AdjustedPriceItemReader.java (신규)
└── entity/
    └── CorpEventHistory.java (기존)
```

#### 기능 요구사항
- 액면분할, 증자, 감자 이벤트 수집
- 과거 주가 자동 보정
- `adj_close_price` 필드 계산 및 저장
- 조정 비율 합리성 검증 (0.5 ~ 2.0)

---

### 2. 기술적 지표 계산 배치

#### 현재 상태
- ❌ 배치 API 없음 (`/batch/indicators`)
- ✅ Ta4j 라이브러리 의존성 있음
- ❌ 지표 계산 로직 미구현

#### 필요한 구현
```
services/stock-price/
├── controller/StockController.java
│   └── POST /batch/indicators (신규)
├── service/StockIndicatorService.java (신규)
├── batchJob/
│   ├── StockIndicatorBatch.java (신규)
│   └── ItemReader/StockIndicatorItemReader.java (신규)
└── entity/
    └── TB_STOCK_INDICATOR (기존)
```

#### 기능 요구사항
- **이동평균선 (MA)**: 5일, 20일, 60일, 120일
- **RSI (14일)**: 0-100 범위 검증
- **MACD**: Signal, Histogram
- **Bollinger Bands**: Upper, Middle, Lower
- **Momentum**: 1개월, 3개월, 6개월
- 최소 300거래일 데이터 확보 검증

---

### 3. 재무 지표 계산 로직 개선

#### 현재 상태
- ⚠️ 재무 정보 수집됨 (2,579개)
- ❌ 재무 지표 미계산 (NULL 97.6%)
- ❌ 주가 데이터 연동 안됨

#### 필요한 수정
```
services/stock-finance/
├── batchJob/CorpFinanceBatch.java
│   └── corpFinanceProcessor() 수정
├── service/CorpFinanceService.java
│   └── calculateIndicators() 수정
└── client/StockClient.java
    └── getLatestStockPrice() 호출 추가
```

#### 기능 요구사항
- **PER** (Price to Earnings Ratio): 시가총액 / 순이익
- **PBR** (Price to Book Ratio): 시가총액 / 자본총계
- **ROE** (Return on Equity): 순이익 / 자본총계
- **ROA** (Return on Assets): 순이익 / 자산총계
- **성장률**: 매출액, 순이익 전년 대비
- Stock code 형식 변환 (`A900100` → `900100`)

---

## 구현 우선순위

### Priority 1: 재무 지표 계산 (즉시)
**이유**: 데이터는 이미 수집되어 있고, 주가 데이터도 있음. 연동만 하면 바로 계산 가능.

**예상 소요 시간**: 1-2시간

**구현 단계**:
1. `CorpFinanceBatch.corpFinanceProcessor()` 수정
2. Stock code 형식 변환 로직 추가
3. `StockClient.getLatestStockPrice()` 호출
4. 재무 지표 계산 및 저장
5. 테스트 및 검증

---

### Priority 2: 기술적 지표 계산 (중요)
**이유**: 백테스팅에 필수적인 기능. Ta4j 라이브러리 이미 있음.

**예상 소요 시간**: 3-4시간

**구현 단계**:
1. `StockIndicatorService` 생성
2. Ta4j 기반 지표 계산 로직 구현
3. `StockIndicatorBatch` 생성
4. `StockIndicatorItemReader` 구현
5. Controller API 추가
6. 테스트 및 검증

---

### Priority 3: 수정주가 계산 (선택)
**이유**: 백테스팅 정확도 향상. 하지만 단기적으로는 원주가로도 가능.

**예상 소요 시간**: 4-5시간

**구현 단계**:
1. 기업 이벤트 수집 API 연동
2. `AdjustedPriceService` 생성
3. 조정 비율 계산 로직 구현
4. `AdjustedPriceBatch` 생성
5. Controller API 추가
6. 테스트 및 검증

---

## 구현 실행 계획

### Step 1: 재무 지표 계산 구현 (Priority 1)

#### 1.1 Stock code 형식 변환 유틸리티 추가
**파일**: `services/stock-finance/src/main/java/com/stock/finance/batchJob/CorpFinanceBatch.java`

```java
// Processor에서 주가 조회 시 Stock code 변환
String stockCode = corpFinance.getCorpCode().replace("A", "");
StockPriceDto stockPrice = stockClient.getLatestStockPrice(stockCode);
```

#### 1.2 재무 지표 계산 로직 추가
**파일**: `services/stock-finance/src/main/java/com/stock/finance/service/CorpFinanceService.java`

```java
// PER = 시가총액 / 순이익
// PBR = 시가총액 / 자본총계
// ROE = 순이익 / 자본총계
// ROA = 순이익 / 자산총계
```

#### 1.3 테스트
```bash
# 2024년 재무 정보 재처리
curl -X POST "http://localhost:8082/batch/corp-fin?date=20240213"

# 결과 확인
docker exec stock-finance-db mysql -u finance_user -pfinance_pass stock_finance -e "
SELECT 
    validation_status,
    COUNT(*) as count
FROM TB_CORP_FINANCE
GROUP BY validation_status;
"
```

**예상 결과**: VERIFIED 비율 90% 이상

---

### Step 2: 기술적 지표 계산 구현 (Priority 2)

#### 2.1 StockIndicatorService 생성
**파일**: `services/stock-price/src/main/java/com/stock/price/service/StockIndicatorService.java`

```java
@Service
public class StockIndicatorService {
    
    public StockIndicator calculateIndicators(String stockCode, LocalDate date) {
        // 1. 과거 300일 주가 데이터 조회
        // 2. Ta4j BarSeries 생성
        // 3. RSI, MACD, Bollinger Bands, Momentum 계산
        // 4. StockIndicator 엔티티 생성 및 반환
    }
}
```

#### 2.2 StockIndicatorBatch 생성
**파일**: `services/stock-price/src/main/java/com/stock/price/batchJob/StockIndicatorBatch.java`

```java
@Configuration
public class StockIndicatorBatch {
    
    @Bean
    public Job stockIndicatorJob() {
        return new JobBuilder("stockIndicatorJob", jobRepository)
                .start(stockIndicatorStep())
                .build();
    }
    
    @Bean
    public Step stockIndicatorStep() {
        return new StepBuilder("stockIndicatorStep", jobRepository)
                .<String, StockIndicator>chunk(CHUNK_SIZE, transactionManager)
                .reader(stockIndicatorItemReader())
                .processor(stockIndicatorProcessor())
                .writer(stockIndicatorWriter())
                .build();
    }
}
```

#### 2.3 Controller API 추가
**파일**: `services/stock-price/src/main/java/com/stock/price/controller/StockController.java`

```java
@PostMapping("/indicators")
@Operation(summary = "기술적 지표 계산", description = "수집된 주가 데이터를 기반으로 기술적 지표를 계산합니다.")
public ResponseEntity<String> calculateIndicators(
        @Parameter(description = "기준 일자 (yyyyMMdd)")
        @Pattern(regexp = "^\\d{8}$", message = "날짜 형식은 yyyyMMdd 형식이어야 합니다")
        @RequestParam(value = "date", required = false) String date) throws Exception {
    
    if (!StringUtils.hasText(date)) {
        date = toLocalDateString(LocalDate.now().minusDays(1));
    }
    
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("date", date)
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
    
    jobLauncher.run(jobRegistry.getJob("stockIndicatorJob"), jobParameters);
    
    return ResponseEntity.ok("BATCH STARTED: Stock indicator calculation for " + date);
}
```

#### 2.4 테스트
```bash
# 기술적 지표 계산
curl -X POST "http://localhost:8083/batch/indicators?date=20240213"

# 결과 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT COUNT(*) as total FROM TB_STOCK_INDICATOR;
SELECT stock_code, indicator_date, rsi_14, macd, momentum_1m 
FROM TB_STOCK_INDICATOR LIMIT 5;
"
```

**예상 결과**: 2,000개 이상 지표 계산

---

### Step 3: 수정주가 계산 구현 (Priority 3)

#### 3.1 AdjustedPriceService 생성
**파일**: `services/stock-price/src/main/java/com/stock/price/service/AdjustedPriceService.java`

```java
@Service
public class AdjustedPriceService {
    
    public void calculateAdjustedPrice(String stockCode, LocalDate date) {
        // 1. 기업 이벤트 조회 (액면분할, 증자, 감자)
        // 2. 조정 비율 계산
        // 3. 과거 주가 보정
        // 4. adj_close_price 업데이트
    }
}
```

#### 3.2 Controller API 추가
```java
@PostMapping("/adjusted-price")
@Operation(summary = "수정주가 계산", description = "기업 이벤트를 반영하여 수정주가를 계산합니다.")
public ResponseEntity<String> calculateAdjustedPrice(...) {
    // 배치 실행
}
```

#### 3.3 테스트
```bash
# 수정주가 계산
curl -X POST "http://localhost:8083/batch/adjusted-price?date=20240213"

# 결과 확인
docker exec stock-price-db mysql -u stock_user -pstock_pass stock_price -e "
SELECT 
    stock_code,
    bas_dt,
    end_price,
    adj_close_price,
    ROUND((adj_close_price / end_price), 4) as adjustment_ratio
FROM TB_STOCK_PRICE 
WHERE adj_close_price IS NOT NULL
LIMIT 10;
"
```

---

## 예상 일정

| 단계 | 작업 | 예상 시간 | 담당 |
|------|------|-----------|------|
| Step 1 | 재무 지표 계산 구현 | 1-2시간 | 개발자 |
| Step 2 | 기술적 지표 계산 구현 | 3-4시간 | 개발자 |
| Step 3 | 수정주가 계산 구현 | 4-5시간 | 개발자 |
| 테스트 | 통합 테스트 및 검증 | 2시간 | 개발자 |
| **총계** | | **10-13시간** | |

---

## 성공 기준

### Step 1 완료 기준
- ✅ 재무 지표 계산 성공률 90% 이상
- ✅ PER, PBR, ROE, ROA 값 합리성 검증
- ✅ Stock code 형식 변환 정상 동작

### Step 2 완료 기준
- ✅ 2,000개 이상 종목 지표 계산
- ✅ RSI 값 0-100 범위 내
- ✅ MACD, Bollinger Bands 값 합리성
- ✅ Momentum 지표 정상 계산

### Step 3 완료 기준
- ✅ 수정주가 커버리지 80% 이상
- ✅ 조정 비율 합리성 (0.5 ~ 2.0)
- ✅ 액면분할 이벤트 반영 확인

---

## 리스크 및 대응

### 리스크 1: Ta4j 라이브러리 사용법 미숙
**대응**: 공식 문서 및 예제 코드 참고, 단순한 지표부터 구현

### 리스크 2: 주가 데이터 부족 (300일 미만)
**대응**: 데이터 부족 시 해당 종목 스킵, 로그 기록

### 리스크 3: API 타임아웃
**대응**: 타임아웃 설정 증가, 재시도 로직 추가

---

## 다음 단계

Phase 1 완료 후:
1. **Phase 2 검증**: 백테스팅 엔진 테스트
2. **Phase 3 시작**: 종목 추천 시스템 설계
3. **문서 업데이트**: Verification_Plan.md 수정

---

**작성자**: AI Assistant  
**검토자**: 개발팀  
**승인자**: 프로젝트 매니저
