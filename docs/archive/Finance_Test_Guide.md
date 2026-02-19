# 재무 데이터 수집 테스트 가이드

**작성일**: 2026-02-15  
**목적**: DART API 로직 검증 및 디버깅

---

## 테스트 엔드포인트

### 단일 기업 재무 정보 조회

**엔드포인트**: `POST /batch/corp-fin/test`

**용도**:
- DART API 연동 로직 검증
- 특정 기업의 재무 데이터 확인
- API 응답 구조 확인
- 디버깅 및 문제 해결

**파라미터**:
- `stockCode` (필수): 종목 코드 (A + 6자리 숫자, 예: A005930)
- `year` (필수): 조회 연도 (2000-2100)

---

## 사용 예시

### 1. 삼성전자 2024년 재무제표 조회

```bash
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A005930&year=2024"
```

**예상 응답**:
```json
[
  {
    "corpCode": "A005930",
    "bizYear": "2024",
    "reportCode": "Q1",
    "basDt": "2024-03-31",
    "totalAssets": 1234567890000,
    "totalDebt": 456789012000,
    "totalCapital": 777778878000,
    "revenue": 123456789000,
    "opIncome": 12345678900,
    "netIncome": 9876543210,
    "operatingCashflow": 15000000000,
    "freeCashflow": 12000000000,
    "ebitda": 14000000000,
    "validationStatus": "VERIFIED"
  },
  {
    "corpCode": "A005930",
    "bizYear": "2024",
    "reportCode": "SEMI",
    ...
  },
  {
    "corpCode": "A005930",
    "bizYear": "2024",
    "reportCode": "Q3",
    ...
  },
  {
    "corpCode": "A005930",
    "bizYear": "2024",
    "reportCode": "ANNUAL",
    ...
  }
]
```

### 2. SK하이닉스 2023년 재무제표 조회

```bash
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A000660&year=2023"
```

### 3. 데이터 없음 케이스 (미래 연도)

```bash
curl -X POST "http://localhost:8082/batch/corp-fin/test?stockCode=A005930&year=2025"
```

**예상 응답**:
```
No financial data found for A005930 (2025). 
Possible reasons: 1) Not yet published, 2) Invalid corp code, 3) API limit exceeded
```

---

## 검증 포인트

### 1. Corp Code 매핑 확인
로그에서 확인:
```
Found corp code: 00126380 for stock: A005930
```

### 2. 4개 분기 데이터 수집
- Q1 (1분기)
- SEMI (반기)
- Q3 (3분기)
- ANNUAL (사업보고서)

### 3. 재무제표 정합성
```
totalAssets = totalDebt + totalCapital
```

### 4. 현금흐름 계산
```
freeCashflow = operatingCashflow - investingCashflow
ebitda = opIncome + depreciation
```

### 5. ValidationStatus
- `VERIFIED`: 정합성 검증 통과
- `WARNING`: 경미한 오차 (허용 범위 내)
- `INVALID`: 정합성 검증 실패

---

## 권장 테스트 케이스

### 대형주 (데이터 안정적)
| 종목 | 코드 | 비고 |
|------|------|------|
| 삼성전자 | A005930 | 시가총액 1위 |
| SK하이닉스 | A000660 | 반도체 |
| 현대차 | A005380 | 자동차 |
| NAVER | A035420 | IT 서비스 |
| 카카오 | A035720 | IT 서비스 |

### 중소형주 (엣지 케이스)
| 종목 | 코드 | 비고 |
|------|------|------|
| 셀트리온 | A068270 | 바이오 |
| LG에너지솔루션 | A373220 | 배터리 |
| 포스코홀딩스 | A005490 | 철강 |

### 테스트 연도
- **2024년**: 모든 분기 공시 완료 (권장)
- **2023년**: 안정적 데이터
- **2022년**: 과거 데이터 검증

---

## 문제 해결

### 에러: "Corp code not found"
**원인**: 종목 코드가 DART 고유번호 매핑에 없음
**해결**: 
1. 종목 코드 형식 확인 (A + 6자리)
2. 상장 폐지 종목인지 확인
3. Corp code 캐시 재다운로드

### 에러: "No data for 2025 Q1"
**원인**: 아직 공시되지 않은 분기
**해결**: 과거 연도(2024년 이하)로 테스트

### 에러: "020 - 사용한도를 초과하였습니다"
**원인**: DART API 일일 한도 초과
**해결**: 다음날 자정 이후 재시도

### 에러: "013 - 조회된 데이타가 없습니다"
**원인**: 
1. 해당 분기 미공시
2. 연결재무제표 없음 (개별재무제표만 존재)
3. 잘못된 고유번호

**해결**: 
1. 공시 일정 확인
2. `fs_div` 파라미터 변경 (CFS → OFS)
3. 고유번호 재확인

---

## 로그 확인

### 성공 케이스
```
INFO: Found corp code: 00126380 for stock: A005930
INFO: Fetching 2024 Q1 for A005930
INFO: Successfully fetched 2024 Q1 - 156 accounts
INFO: Fetching 2024 SEMI for A005930
INFO: Successfully fetched 2024 SEMI - 156 accounts
...
```

### 실패 케이스
```
WARN: Corp code not found for stock: A999999
```

```
WARN: DART API returned non-success status: 013 - 조회된 데이타가 없습니다.
WARN: No data for 2025 Q1
```

---

## 전체 배치 vs 테스트 엔드포인트

| 구분 | 전체 배치 | 테스트 엔드포인트 |
|------|----------|-----------------|
| 엔드포인트 | `/batch/corp-fin` | `/batch/corp-fin/test` |
| 대상 | 전체 기업 (2,700개) | 단일 기업 |
| 소요 시간 | 40-50분 | 1초 미만 |
| API 호출 | 10,800건 | 4건 |
| 용도 | 실제 데이터 수집 | 로직 검증, 디버깅 |
| 권장 시점 | 일일 1회 | 개발/테스트 시 수시 |

---

## 다음 단계

1. **단일 기업 테스트**: 대형주 5개로 로직 검증
2. **소량 배치 테스트**: 10개 기업으로 배치 로직 검증
3. **전체 배치 실행**: 검증 완료 후 전체 데이터 수집

---

**작성자**: Kiro AI  
**최종 수정**: 2026-02-15
