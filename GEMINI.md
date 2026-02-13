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
- **Phase 1 (완료)**: 데이터 무결성 검증, 수정주가 계산, 기술적 지표 사전 산출 엔진 구축.
- **Phase 2 (완료)**: `stock-strategy` 서비스 신설, 리밸런싱 시뮬레이션 및 CAGR, MDD 등 퀀트 성과 분석 지표 산출.

## 최근 변경사항 및 수정 (2026-02-12)

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

## 코딩 가이드라인 (Coding Guidelines)

### 1. 종목 코드 규칙
- **기업 정보 (TB_CORP_INFO)**: 'A' 접두사 포함 (예: `A900100`)
- **주가 데이터 (TB_STOCK_PRICE)**: 숫자만 사용 (예: `900100`)
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

*마지막 업데이트: 2026-02-12 (Enhanced Filtering & Sector Implementation)*
