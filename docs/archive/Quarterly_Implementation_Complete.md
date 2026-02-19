# 분기별 재무 데이터 수집 구현 완료

## 구현 내용

### 1. DB 스키마 변경
- **Primary Key 변경**: `(corp_code, bas_dt)` → `(corp_code, biz_year, report_code)`
- **report_code 컬럼 추가**: 보고서 구분 (11013:1Q, 11012:2Q, 11014:3Q, 11011:연간)
- **QoQ/YoY 지표 컬럼 추가**: 6개 성장률 지표

### 2. Entity 수정
- `CorpFinanceId`: 복합키에 `bizYear`, `reportCode` 추가
- `CorpFinance`: `reportCode` 필드 추가, `basDt`는 보조 필드로 변경
- `CorpFinanceIndicator`: QoQ/YoY 성장률 필드 6개 추가

### 3. 데이터 수집 로직
- **4개 보고서 수집**: 1분기, 반기, 3분기, 연간
- **DART API 호출**: 각 기업당 4번 호출 (100ms 딜레이)
- **예상 데이터량**: 2,500개 → 10,000개 (4배 증가)

### 4. 분기 실적 계산 서비스
- `QuarterlyFinanceService` 생성
- **분기 단독 실적 계산**: 누적 데이터에서 이전 분기 차감
- **QoQ 성장률**: 전분기 대비 성장률 (매출, 영업이익, 순이익)
- **YoY 성장률**: 전년 동기 대비 성장률 (매출, 영업이익, 순이익)

### 5. 분기별 조회 API
- `QuarterlyFinanceController` 생성
- **GET /quarterly/{corpCode}/{year}**: 연도별 전체 분기 조회
- **GET /quarterly/{corpCode}/{year}/{reportCode}**: 특정 분기 조회
- **GET /quarterly/{corpCode}/{year}/{reportCode}/standalone**: 분기 단독 실적
- **GET /quarterly/{corpCode}/{year}/{reportCode}/qoq**: QoQ 성장률
- **GET /quarterly/{corpCode}/{year}/{reportCode}/yoy**: YoY 성장률

## 데이터 구조

### 보고서 코드
| 코드 | 보고서 | 기준일 | 데이터 특성 |
|------|--------|--------|------------|
| 11013 | 1분기 | 3월 31일 | 1~3월 누적 |
| 11012 | 반기 | 6월 30일 | 1~6월 누적 |
| 11014 | 3분기 | 9월 30일 | 1~9월 누적 |
| 11011 | 연간 | 12월 31일 | 1~12월 누적 |

### 분기 단독 실적 계산
```
Q1 = 1분기 누적
Q2 = 반기 누적 - 1분기 누적
Q3 = 3분기 누적 - 반기 누적
Q4 = 연간 누적 - 3분기 누적
```

### 성장률 계산
```
QoQ = (현재 분기 - 이전 분기) / 이전 분기 × 100
YoY = (현재 분기 - 전년 동기) / 전년 동기 × 100
```

## 수집 지표

### 기존 지표 (10개)
- PER, PBR, PSR, PCR
- ROE, ROA
- EV/EBITDA, FCF Yield
- Operating Margin, Net Margin

### 신규 지표 (6개)
- QoQ 매출 성장률
- QoQ 영업이익 성장률
- QoQ 순이익 성장률
- YoY 매출 성장률
- YoY 영업이익 성장률
- YoY 순이익 성장률

**총 16개 재무 지표 제공**

## 사용 예시

### 1. 2024년 전체 분기 조회
```bash
GET /quarterly/00123456/2024
```

### 2. 2024년 1분기 단독 실적
```bash
GET /quarterly/00123456/2024/11013/standalone
```

### 3. 2024년 2분기 QoQ 성장률
```bash
GET /quarterly/00123456/2024/11012/qoq
```

### 4. 2024년 3분기 YoY 성장률
```bash
GET /quarterly/00123456/2024/11014/yoy
```

## 배치 실행

```bash
# 2024년 전체 분기 데이터 수집
curl -X POST "http://localhost:8082/batch/corp-fin?date=20240101"

# 예상 소요 시간: 약 40-50분 (2,500개 × 4분기 × 100ms)
```

## 퀀트 전략 활용

### 1. 분기 실적 서프라이즈
- 예상 대비 실적 초과 종목 매수
- QoQ, YoY 동시 성장 종목 선별

### 2. 계절성 전략
- 분기별 매출 패턴 분석
- 성수기 진입 종목 선별

### 3. 실적 모멘텀
- 4분기 연속 성장 종목
- YoY 가속 종목 (성장률이 증가하는 종목)

### 4. 리스크 관리
- 분기별 실적 악화 조기 감지
- 분기별 리밸런싱 근거 제공

## 마이그레이션 파일

1. `V1.2__add_cashflow_columns.sql`: 현금흐름 컬럼 추가
2. `V1.3__add_cashflow_indicators.sql`: 현금흐름 지표 추가
3. `V1.4__add_quarterly_support.sql`: 분기별 지원 (PK 변경)
4. `V1.5__add_quarterly_indicators.sql`: QoQ/YoY 지표 추가

## 주의사항

- 기존 데이터는 `report_code='11011'` (연간)로 자동 설정됨
- 분기 데이터 수집 후 재무 지표 재계산 필요
- QoQ는 1분기에 대해서는 계산 불가 (이전 분기 없음)
- YoY는 전년 데이터가 있어야 계산 가능
