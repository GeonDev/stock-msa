# 검증 과정에서 발견된 이슈 및 수정사항

**작성일**: 2026-02-15  
**검증 버전**: Phase 0-2

---

## 1. 데이터베이스 스키마 불일치

### 문제
검증 계획 문서의 SQL 쿼리가 실제 테이블 구조와 다름

### 상세 내역

**TB_STOCK_PRICE 테이블**:
- ❌ 문서: `trade_date`, `close_price`
- ✅ 실제: `bas_dt`, `end_price`

**실제 컬럼 구조**:
```sql
bas_dt              date            -- 기준일자
start_price         decimal(25,4)   -- 시가
end_price           decimal(25,4)   -- 종가
high_price          decimal(25,4)   -- 고가
low_price           decimal(25,4)   -- 저가
volume              decimal(25,4)   -- 거래량
adj_close_price     decimal(25,4)   -- 수정종가
```

### 수정 완료
- ✅ `Verification_Plan.md` 전체 SQL 쿼리 수정
- ✅ 컬럼명 통일: `bas_dt`, `end_price` 사용

---

## 2. API 엔드포인트 불일치

### 문제
문서에 명시된 엔드포인트가 실제 구현과 다름

### 상세 내역

**기업 상세 정보 수집**:
- ❌ 문서: `POST /batch/corp-detail?date=20240213`
- ✅ 실제: `POST /batch/corp-detail/sector-update` (날짜 파라미터 없음)

**실제 구현된 엔드포인트** (`CorpInfoController.java`):
```java
@PostMapping("/batch/corp-info")              // 기업 기본 정보
@PostMapping("/batch/corp-detail/cleanup")    // 기업 상태 정리
@PostMapping("/batch/corp-detail/sector-update")  // 업종 정보 수집
```

### 수정 완료
- ✅ `Verification_Plan.md` 엔드포인트 경로 수정
- ✅ 업종 정보 수집으로 검증 목표 변경
- ✅ TB_CORP_DETAIL 테이블 대신 TB_CORP_INFO.industry 컬럼 검증

---

## 3. 주가 데이터 수집 방식

### 문제
문서에 단일 배치로 명시되었으나 실제는 시장별 분리 실행

### 상세 내역

**실제 구현**:
- 시장별 개별 배치 실행 필요
- `market` 파라미터 필수: KOSPI, KOSDAQ, KONEX

**올바른 실행 방법**:
```bash
curl -X POST "http://localhost:8083/batch/price?market=KOSPI&date=20240213"
curl -X POST "http://localhost:8083/batch/price?market=KOSDAQ&date=20240213"
curl -X POST "http://localhost:8083/batch/price?market=KONEX&date=20240213"
```

### 수정 완료
- ✅ `Verification_Plan.md` 시장별 순차 실행으로 수정
- ✅ 각 배치 간 대기 시간 추가 (180초)

---

## 4. 루트 엔드포인트 에러

### 문제
서비스 루트 경로(`/`) 접근 시 500 에러 발생

### 원인
정적 리소스가 없는 상태에서 루트 경로 요청 시 `NoResourceFoundException` 발생

### 해결
- ✅ 루트 엔드포인트 추가 (개발자 확인 완료)
- 서비스 상태 확인은 실제 API 엔드포인트 사용 권장

---

## 5. 검증 완료 항목

### 0단계: 환경 초기화
- ✅ Docker 볼륨 완전 삭제
- ✅ 전체 서비스 재빌드 및 시작
- ✅ 초기 스키마 생성 확인

### 1단계: 인프라 확인
- ✅ 11개 컨테이너 정상 실행
- ✅ 모든 DB 연결 성공
- ✅ 서비스 응답 확인

### 2단계: 데이터 수집 (부분 완료)
- ✅ 기업 정보 수집: 2,654개 (KOSPI 837, KOSDAQ 1,687, KONEX 130)
- ✅ 주가 데이터 수집: 2,793개 (3개 시장 통합)
- ⏳ 재무 정보 수집: 대기 중
- ⏳ 수정주가 계산: 대기 중
- ⏳ 기술적 지표 계산: 대기 중

---

## 6. 다음 검증 단계

1. **재무 정보 수집** (DART API)
   - Corp Code 매핑 확인
   - 재무제표 정합성 검증
   - ValidationStatus 분포 확인

2. **수정주가 계산**
   - adj_close_price 필드 계산
   - 조정 비율 합리성 검증

3. **기술적 지표 계산**
   - RSI, MACD, Bollinger Bands, Momentum
   - 지표 커버리지 확인

4. **재무 지표 자동 계산**
   - PER, PBR, ROE, ROA
   - PCR, EV/EBITDA, FCF Yield
   - QoQ, YoY 성장률

5. **백테스팅 엔진**
   - 4가지 전략 검증
   - 성과 지표 계산
   - 입력값 검증 시스템

---

## 7. 권장사항

### 문서 관리
- 실제 구현과 문서 동기화 필요
- API 엔드포인트 변경 시 문서 즉시 업데이트
- 테이블 스키마 변경 시 검증 계획 수정

### 검증 프로세스
- 각 단계별 로그 모니터링 강화
- 배치 실행 간 충분한 대기 시간 확보
- 에러 발생 시 즉시 원인 분석

### 데이터 품질
- Stock code 형식 일관성 유지
- 컬럼명 네이밍 컨벤션 통일
- 필수 필드 NULL 체크 강화

---

## 8. DART API 데이터 조회 실패 원인 분석 (2026-02-15)

### 문제 상황
- **에러 코드**: 013 - 조회된 데이타가 없습니다
- **후속 에러**: 020 - 사용한도를 초과하였습니다
- **영향**: 2,744개 기업 × 4개 분기 = 10,976건 모두 실패

### 근본 원인

#### 1. 미래 연도 재무제표 조회 시도
```java
// 배치 호출: date=20251014
String bizYear = date.substring(0, 4);  // "2025"

// DART API 호출
dartClient.getFinancialStatement(corpCode, "2025", reportCode, "CFS");
```

**문제점**:
- 2025년 10월 14일 기준으로 **2025년 재무제표** 조회
- 2025년 사업보고서는 **2026년 3월**에 공시 예정
- 2025년 1분기 보고서도 **2025년 5월**에 공시 예정
- **결과**: 모든 요청이 "조회된 데이터 없음" 반환

#### 2. 재무제표 공시 일정
| 보고서 | 결산 기준일 | 공시 시점 | 예시 |
|--------|------------|----------|------|
| 1분기 (Q1) | 3월 31일 | 5월 중순 | 2025년 Q1 → 2025년 5월 공시 |
| 반기 (SEMI) | 6월 30일 | 8월 중순 | 2025년 반기 → 2025년 8월 공시 |
| 3분기 (Q3) | 9월 30일 | 11월 중순 | 2025년 Q3 → 2025년 11월 공시 |
| 사업보고서 (ANNUAL) | 12월 31일 | 익년 3월 | 2025년 연간 → 2026년 3월 공시 |

#### 3. API 사용량 폭증
- 모든 요청이 실패하면서도 API 호출 횟수는 증가
- 10,976건 호출 후 일일 한도(10,000건) 초과
- 이후 모든 요청이 "사용량 초과" 에러 반환

### 해결 방안

#### 즉시 조치: 과거 연도 데이터로 테스트
```bash
# 2024년 데이터로 재수집 (이미 공시 완료)
curl -X POST "http://localhost:8082/batch/corp-fin?date=20241014"
```

**2024년 데이터 공시 상태** (2025년 10월 기준):
- ✅ 2024년 Q1 (2024년 5월 공시 완료)
- ✅ 2024년 반기 (2024년 8월 공시 완료)
- ✅ 2024년 Q3 (2024년 11월 공시 완료)
- ✅ 2024년 연간 (2025년 3월 공시 완료)

#### 근본 해결: 로직 개선 필요

**1. 연도 검증 추가**
```java
// 미래 연도 요청 차단
int requestYear = Integer.parseInt(bizYear);
int currentYear = LocalDate.now().getYear();
if (requestYear >= currentYear) {
    throw new IllegalArgumentException("Cannot fetch financial data for future year: " + requestYear);
}
```

**2. 분기별 공시 시점 체크**
```java
// 아직 공시되지 않은 분기는 스킵
LocalDate publicationDate = getPublicationDate(reportCode, year);
if (LocalDate.now().isBefore(publicationDate)) {
    log.info("Skipping {} {} - not yet published", year, reportCode);
    continue;
}
```

**3. 조기 중단 로직**
```java
// 연속 실패 시 배치 중단
int consecutiveFailures = 0;
if (consecutiveFailures > 100) {
    log.error("Too many consecutive failures, stopping batch");
    break;
}
```

**4. DataGo API Fallback 강화**
```java
// DART 실패 시 DataGo로 자동 전환
if (dartResponse == null || dartResponse.getList().isEmpty()) {
    log.info("DART failed, falling back to DataGo API");
    return getCorpFinanceFromDataGo(bizYear);
}
```

### 권장 테스트 날짜

| 목적 | 권장 날짜 | 이유 |
|------|----------|------|
| 전체 검증 | 2024-10-14 | 2024년 모든 분기 공시 완료 |
| 최신 데이터 | 2025-09-30 | 2025년 Q1, 반기 공시 완료 |
| 부분 검증 | 2023-12-31 | 2023년 전체 데이터 안정적 |

### 검증 문서 업데이트 필요 사항
- ✅ DART API 사용량 제한 추가
- ✅ 재무제표 공시 일정 안내 추가
- ⏳ 권장 테스트 날짜 가이드 추가
- ⏳ 에러 처리 로직 개선 필요

---

**검증 진행률**: 약 30% (0-2단계 중 2.3까지 완료)  
**예상 완료 시간**: 약 90분 추가 소요 (DART API 한도 리셋 후)
