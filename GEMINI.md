# Gemini Project Analysis: stock-msa

## 프로젝트 개요

`stock-msa`는 단일 배치 서비스 구조에서 도메인 중심의 **MSA(Microservice Architecture)** 환경으로 리팩토링된 프로젝트입니다. 한국 공공데이터포털과 DART API를 통해 데이터를 수집하여 **동적 자산배분(퀀트) 투자**를 지원하는 시스템을 구축하고 있으며, **Gradle Multi-Module Monorepo** 구조를 취하고 있습니다.

## 주요 기술 스택

- **Core:** Java 21 (Amazon Corretto), Spring Boot 3.4.8, Spring Cloud 2024.0.2
- **Microservices Infrastructure:**
  - **Service Discovery:** Spring Cloud Netflix Eureka (Port: 8761)
  - **API Gateway:** Spring Cloud Gateway (Port: 8080)
- **Data Access:** Spring Data JPA, Spring Batch, MySQL 8.0, Flyway
- **Communication & Serialization:** RestClient, Jackson JSR310 (JavaTimeModule)
- **Utilities:** Ta4j (기술적 분석), TinyLog 2.6.2, Lombok, dotenv-java, SpringDoc OpenAPI 2.8.4
- **Containerization:** Docker, Docker Compose (Healthcheck 적용)

## 주요 아키텍처 및 특징

### 1. 보안 및 인증 (Security)
- **서비스별 독립 계정**: `EUREKA_USER`, `GATEWAY_USER` 등 각 인프라 서비스의 접근 권한을 분리했습니다.
- **Gateway Routing**: `/api/v1/{domain}/**` 패턴을 통해 각 서비스(`corp`, `finance`, `price`, `strategy`)로 라우팅됩니다.

### 2. 데이터베이스 및 엔티티 매핑
- **독립 데이터베이스**: 각 서비스는 전용 DB(예: `stock_corp`, `stock_finance`)와 공유 배치 DB(`stock_batch`)를 사용합니다.
- **금융 데이터 정밀도**: 모든 주가, 거래량, 재무 항목 및 지표는 오버플로우 방지를 위해 `BigDecimal`(`DECIMAL(25, 4)`) 타입을 사용합니다.

### 3. Spring Batch 운영 정책
- **ItemReader 독립화 (필수)**: 모든 배치 `ItemReader`는 `batchJob/ItemReader/` 패키지의 독립 클래스로 분리하여 관리합니다.
- **Chunk-Oriented Batch**: 대용량 데이터 처리를 위해 Chunk 기반 프로세싱을 표준으로 하며, `ApplicationConstants`를 통해 Chunk Size를 중앙 제어합니다.

### 4. 개발 단계 (Milestones)
- **Phase 1 (완료)**: 데이터 무결성 검증, 수정주가 계산, 기술적 지표 사전 산출, DART API 전환 및 재무 지표 자동 계산 (2026-02-23 검증 완료).
- **Phase 2 (완료)**: `stock-strategy` 서비스 내 백테스팅 시뮬레이션 구축, 거래 비용(슬리피지, 수수료, 세금) 모델링, 단일 종목 최대 비중 제약 및 파라미터 그리드 서치(최적화) 기능 완성 (2026-02-24 완료).

## 최근 변경사항 및 수정 (2026-02-24)

### 1. 백테스팅 엔진 고도화 및 최적화 기능 (Phase 2 완료) ✅
**목적**: 전략 간 비교 및 파라미터 자동 최적화, 보다 정교한 거래 시뮬레이션 환경 구축

**주요 구현 사항**:
- **전략 비교 API (`/api/v1/strategy/backtest/compare`)**: 다수의 백테스팅 결과를 비교하여 최고 CAGR, 최고 Sharpe, 최저 MDD 등의 최적 결과를 도출.
- **파라미터 그리드 서치 (`/api/v1/strategy/backtest/optimize`)**: Top N 종목 수, PBR/PER/ROE 가중치 범위 등을 스텝 단위로 자동 순회하며 최적 파라미터 백테스팅 병렬 수행.
- **거래 제약 (Constraints)**: 단일 종목 자본 집중 방지를 위한 최대 비중(ex. 20%) 제한, 최소 매수 단위(소수점 불가), 예산 초과 방어(잔고 내 매수) 모델 적용.
- **슬리피지 (Slippage) 모델링**: `SlippageModel` 인터페이스 기반으로 고정 슬리피지(Fixed) 및 거래량 기반 슬리피지(Volume) 로직을 적용하여 실제 체결 단가 구현.
- **DB 업데이트**: `TB_BACKTEST_RESULT` 테이블에 `is_optimized` (그리드 서치 판별 여부) 및 `slippage_type` (적용된 슬리피지 모델) 상태 컬럼 추가 (Flyway `V2__add_backtest_fields.sql`).

## 최근 변경사항 및 수정 (2026-02-23)

### 1. 재무 데이터 배치 버그 수정 및 구조 개선 ✅
**목적**: DB 저장 0건 버그 수정 및 DART API 일일 한도 대응

**주요 수정사항**:
- **DB 저장 0건 버그 수정**:
  - 원인: `CorpFinanceItemReader`의 불필요한 필터링으로 전체 데이터 제외
  - 해결: 필터링 로직 제거 (DART API에서 이미 상장사만 조회)
  
- **분기별 개별 수집 API 추가**:
  - DART API 일일 한도(10,000건) 초과 방지
  - 4개 분기 전체(10,776 호출) → 분기별 개별(2,694 호출)
  - API: `POST /batch/corp-fin?date=20241014&reportCode=Q1`
  
- **CorpFinance 엔티티 구조 단순화**:
  - 복합키(`@IdClass`) → auto-increment PK 전환
  - `@Convert` 적용 문제 해결 (ReportCode Enum)
  - `UNIQUE KEY (corp_code, biz_year, report_code)` 유일성 보장
  - DB 마이그레이션: `V2__simplify_pk.sql`

**삭제된 파일**:
- `CorpFinanceId.java`
- `CorpFinanceIndicatorId.java`

### 2. 재무 지표 계산 로직 수정 ✅
**문제**: 배치 프로세서에서 기업 정보 조회 실패로 재무 지표 미계산

**해결**:
- `CorpFinanceBatch.corpFinanceProcessor()` 수정
- 불필요한 `corpClient.getCorpInfo()` 호출 제거
- `CorpFinance.corpCode` 필드가 실제로는 stockCode를 저장함을 반영
- 직접 주가 조회: `stockClient.getLatestStockPrice(stockCode)`
- 예외 처리 추가: 지표 계산 실패 시에도 재무 데이터 저장

**결과**:
- 재무 지표 계산 성공률: 99% 이상
- PER: 64.2% (순이익 양수인 경우만)
- PBR: 99.6%
- ROE: 99.4%
- ROA: 99.4%

### 3. 데이터 품질 검증 완료 ✅
**Q1 2024 재무 데이터 (2026-02-23 기준)**:
- 총 수집: 2,495건
- VERIFIED: 2,337건 (93.7%)
- ERROR_MISSING: 157건 (6.3%)
- ERROR_IDENTITY: 1건 (0.04%)

**재무 지표 저장**:
- 총 저장: 2,495건 (100%)
- PER: 1,603건 (64.2%)
- PBR: 2,485건 (99.6%)
- ROE: 2,481건 (99.4%)
- ROA: 2,480건 (99.4%)

### 4. Flyway 마이그레이션 수정 ✅
**문제**: V2 마이그레이션 파일 SQL 문법 오류

**수정**:
- `V2__simplify_pk.sql` 첫 줄 수정
- `DROP TABLE IF EXISTS` 구문 정상화
- Flyway 스키마 히스토리 정리

---

## 최근 변경사항 및 수정 (2026-02-19)

### 1. DART API Rate Limiting 구현 ✅
- **목적**: DART API IP 차단 방지 (분당 1,000회 제한)
- **구현 완료**:
  - `DartRateLimiter`: Sliding window 방식의 rate limiter
  - `stock-corp/DartClient`: 기업 정보 조회 시 rate limiting 적용
  - `stock-finance/DartClient`: 재무제표 조회 시 rate limiting 적용
- **특징**:
  - 분당 정확히 1,000회 이하 보장
  - Thread-safe (synchronized)
  - 자동 대기 및 타임스탬프 관리
  - 1시간 IP 차단 위험 제거

### 2. 코드 품질 개선 (Clean Code)
- **DartFinanceConverter 리팩토링**:
  - `convertToCorpFinance` 메서드 분리 (100줄 → 15줄)
  - `extractBalanceSheet`: 재무상태표 추출
  - `extractIncomeStatement`: 손익계산서 추출
  - `extractCashflowStatement`: 현금흐름표 추출
  - `calculateDerivedMetrics`: FCF, EBITDA 계산
- **효과**: 가독성, 유지보수성, 테스트 용이성 향상

### 3. Pre-commit Hook 개선
- **AI 환각 방지 강화**:
  - Swagger/Lombok 어노테이션 정상 인식
  - 실제 컴파일 오류만 차단
  - 코드 스타일 제안은 BLOCK 금지
- **클린코드 섹션 추가**: `###CLEAN###`
  - 네이밍, 메서드 길이, 중복 코드, 복잡도 분석
  - 커밋 차단 없이 개선 제안만 제공

### 4. CorpClient 주석 명확화
- `getDartCorpCode` 메서드 주석 수정
- "DB 조회" → "stock-corp 서비스 API 호출 → DB 조회"
- 실제 동작 흐름 명확히 표현

---

## 최근 변경사항 및 수정 (2026-02-16)

### 1. DART API 전환 완료 ✅
- **목적**: 불안정한 DataGo API에서 공식 DART API로 재무 데이터 수집 전환
- **구현 완료** (2026-02-23 검증 완료):
  - `DartClient`: DART API 호출 (단일 메서드로 간소화, 67줄)
  - `DartFinanceConverter`: DART 계정과목 → CorpFinance 엔티티 변환
  - `CorpFinanceService`: DART 전용 (DataGo fallback 제거)
  - `CorpFinanceBatch`: 재무 데이터 수집 및 지표 계산 통합
  - DART Corp Code DB 저장 (`TB_CORP_INFO.dart_corp_code`)
  - `CorpInfoBatch`: 기업 정보 수집 시 DART Corp Code 자동 매핑
- **데이터 흐름**:
  1. DART Corp Code XML 다운로드 및 파싱 → DB 저장
  2. Stock Code → Corp Code 매핑
  3. DART API 호출 (100ms 딜레이, 분기별 개별 수집)
  4. 계정과목 파싱 (BS, IS, CF)
  5. 주가 데이터 조회 (stock-price 서비스)
  6. FCF, EBITDA 자동 계산
  7. 재무 지표 계산 (16개 지표: PER, PBR, ROE, ROA, 성장률 등)
  8. DB 저장 및 검증 (대차대조표 등식, 필수 필드)
- **데이터 품질**:
  - 재무 데이터: 93.7% VERIFIED
  - 재무 지표: 99% 이상 계산 성공

### 2. DART Corp Code DB 저장
- **변경**: 로컬 캐시 파일 → DB 기반 관리
- **추가 컬럼**: `TB_CORP_INFO.dart_corp_code VARCHAR(8)` + 인덱스
- **장점**:
  - 로컬 파일 의존성 제거
  - 서비스 재시작 시에도 데이터 유지
  - 여러 인스턴스 간 데이터 공유
  - 빠른 조회 (인덱스 활용)

### 3. 코드 간소화 및 정리
- **DartClient**: 245줄 → 67줄 (73% 감소)
  - 사용하지 않는 메서드 제거
  - 단일 책임 원칙 적용
- **CorpFinanceService**: DataGo API 관련 코드 제거
  - fallback 로직 제거
  - 미사용 필드/import 제거
- **외부 API 관리 통일**: ApplicationConstants로 중앙 관리

### 4. 문서 정리 및 통합
- **3개 핵심 문서**:
  - `docs/Testing_and_Verification.md`: 검증 가이드
  - `docs/Implementation_Status.md`: 구현 현황
  - `docs/Implementation_Roadmap.md`: 구현 계획
- **통합 완료**: 9개 파편화된 문서 → 3개 핵심 문서
- **스티어링 문서**: `.kiro/steering/docs.md` 등록

### 5. 검증 프로세스 개선
- **재무 데이터 수집**: 단일 기업 테스트 → 전체 배치 실행
- **테스트 엔드포인트**: `/batch/corp-fin/test?stockCode=A005930&year=2024`
- **검증 항목**:
  - DART Corp Code 매핑 성공
  - 4개 분기 데이터 수집 완료
  - 재무제표 검증 통과 (VERIFIED)
  - 재무 지표 계산 완료

### 이전 변경사항 (2026-02-15)

- **API 문서화 강화**: `stock-common` 및 `stock-strategy` 모듈의 모든 DTO에 Swagger(`@Schema`) 어노테이션을 추가하여 필드별 설명과 예시 값을 명시했습니다.
- **유니버스 필터링 엔진 고도화**:
    - `Map<String, Object>` 기반의 불투명한 필터 구조를 `CustomFilterCriteria` DTO로 정규화하여 타입 안정성을 확보했습니다.
    - **재무 지표 필터**: PER, PBR, ROE, PSR, 부채비율 및 흑자여부(`onlyProfitable`) 필터링 기능을 구현했습니다.
    - **기술적 지표 필터**: RSI, MACD 및 이동평균선(20/60/120일) 대비 가격 위치 기반 필터링을 추가했습니다.
    - **성능 최적화**: 서비스 간 통신 시 Batch API를 활용하여 대량 종목의 지표를 효율적으로 조회하도록 개선했습니다.
- **업종 분류 시스템 및 자동화 구현**:
    - GICS 표준 기반의 `SectorType` Enum을 정의하고 `CorpDetail` 엔티티 및 DTO에 반영했습니다.
    - `TB_CORP_DETAIL` 테이블에 `sector` 컬럼을 추가하는 Flyway 마이그레이션(`V1.1`)을 완료했습니다.
    - **OpenDART 기반 자동 수집**: `induty_code`(한국표준산업분류, KSIC)를 활용한 업종 정보 자동 수집 배치(`SectorUpdateBatch`)를 구현했습니다.
    - **코드 기반 정밀 매핑**: KSIC 중분류 코드(앞 2자리)를 `SectorType`으로 변환하는 매핑 로직을 `stock-common` 모듈에 구축했습니다.

### 5. 서비스 모니터링 및 식별 (New)
- **루트 엔드포인트 (`/`)**: 모든 서비스에 서비스 이름, 버전(`0.0.1-SNAPSHOT`), 활성 프로파일 정보를 반환하는 엔드포인트를 구현했습니다.
- **공통 DTO**: `ServiceInfoDto`를 `stock-common` 모듈에 정의하여 모든 서비스에서 일관된 응답 형식을 유지합니다.
- **운영 편의성**: Docker Healthcheck 및 배포 시 서비스 식별을 용이하게 합니다.

## 코딩 가이드라인 (Coding Guidelines)

### 1. 종목 코드 규칙
- **기업 정보 (TB_CORP_INFO)**: 'A' 접두사 포함 (예: `A900100`)
  - `corp_code`: 사업자등록번호 기반 고유번호 (예: `1301110006246`)
  - `stock_code`: KRX 종목코드 (예: `A005930`)
  - `dart_corp_code`: DART 고유번호 (예: `00126380`)
- **주가 데이터 (TB_STOCK_PRICE)**: 숫자만 사용 (예: `900100`)
- **재무 데이터 (TB_CORP_FINANCE)**: `corp_code` 필드에 stock_code 저장 (예: `A005930`)
- 주가 조회 시 반드시 `corpInfo.getStockCode().replace("A", "")`를 통해 접두사를 제거해야 합니다.

### 2. 서비스 간 통신 (RestClient)
- 인터페이스 기반의 `RestClient`를 사용하며, Docker 환경에서는 서비스 이름(예: `stock-price:8083`)을, 로컬에서는 `localhost`를 사용하도록 `application.yaml` 프로필로 관리합니다.

### 3. 데이터 처리
- 모든 날짜는 `LocalDate`를 사용하며, 금융 계산에는 반드시 `BigDecimal`과 명시적인 Rounding 모드를 사용합니다.
- 가독성을 위해 메서드는 30라인 이내로 유지하고, 비즈니스 로직에 대한 **Why(이유)** 위주의 주석을 작성합니다.

## 프로젝트 구조

```
stock-msa/
├── modules/stock-common/     # 공통 DTO, 예외 처리, 유틸리티
├── services/
│   ├── stock-discovery/      # Eureka 서비스 레지스트리 (8761)
│   ├── stock-gateway/        # 통합 API 게이트웨이 (8080)
│   ├── stock-corp/           # 기업 정보 관리 (8081)
│   ├── stock-finance/        # 재무제표 및 지표 (8082)
│   ├── stock-price/          # 주가 및 기술적 지표 (8083)
│   └── stock-strategy/       # 백테스팅 및 전략 (8084)
├── data/                     # Docker DB Volume 마운트 포인트
└── docker-compose.yaml       # 전체 서비스 오케스트레이션
```

## 실행 및 빌드

- **환경 설정**: 루트의 `.env` 파일에 API 키 및 DB 비밀번호 설정 필수.
- **전체 기동**: `docker-compose up -d --build`
- **로컬 개발**: `local` 프로필 활성화 (`-Dspring.profiles.active=local`) 시 Eureka 없이 독립 실행 가능.

---

*마지막 업데이트: 2026-02-23 (재무 데이터 배치 버그 수정, 재무 지표 계산 로직 개선, 데이터 품질 검증 완료)*
