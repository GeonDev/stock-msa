# Gemini Project Analysis: stock-msa

## 프로젝트 개요

`stock-msa`는 단일 배치 서비스 구조에서 도메인 중심의 **MSA(Microservice Architecture)** 환경으로 리팩토링된 프로젝트입니다. 주식 정보를 수집하여 **동적 자산배분(퀀트) 투자**에 도움이 되는 시스템 구축을 목표로 하며, **Gradle Multi-Module Monorepo** 구조를 취하고 있습니다. 또한 **Docker 기반의 통합 개발 환경**과 **중앙 집중식 설정 관리**를 제공합니다.

## 주요 기술 스택

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 21 (Amazon Corretto)
- **Build Tool:** Gradle (Multi-Module)
- **Microservices Support:**
  - **Service Discovery:** Spring Cloud Netflix Eureka (Port: 8761)
  - **API Gateway:** Spring Cloud Gateway (**Reactive/WebFlux**) (Port: 8080)
- **Security:** Spring Security (HTTP Basic Auth, 서비스별 계정 분리)
- **Data Access:** Spring Data JPA, Spring Batch, MySQL, Flyway
- **Containerization:** Docker, Docker Compose (Healthcheck 적용)
- **Utilities:** Tinylog, Lombok, dotenv-java

## 주요 아키텍처 및 특징

### 1. 보안 및 인증 (Security)
- **서비스별 독립 계정**: `EUREKA_USER`, `GATEWAY_USER`를 통해 각 인프라 서비스의 접근 권한을 분리했습니다.
- **Reactive Security**: Gateway는 WebFlux 기반의 `SecurityWebFilterChain`을 사용하여 비동기 논블로킹 환경에 최적화된 보안을 제공합니다.

### 2. Docker 통합 환경
- `amazoncorretto:17-alpine` 이미지를 사용하여 Apple Silicon(M1/M2) 및 일반 x86 환경 모두를 지원합니다.
- `depends_on` 및 `healthcheck`를 통해 서비스 간 기동 의존성(Discovery -> Apps)을 보장합니다.
- **데이터 영속성 보장**: 각 DB 컨테이너는 Docker Volume(`*-db-volume`)을 마운트하여 컨테이너가 삭제되어도 데이터가 유실되지 않도록 설정했습니다.

### 3. 데이터베이스 및 엔티티 매핑
- **멀티 데이터소스 설정**: 각 서비스는 `DbConfig`를 통해 메인 DB와 공유 배치 DB(`stock_batch`)를 가집니다. `DataSourceProperties`를 사용하여 Spring Boot의 표준 프로퍼티 바인딩 방식을 준수합니다.
- **명시적 컬럼 매핑**: DB 스키마(snake_case)와 Java 엔티티(camelCase) 간의 일관성을 위해 모든 필드에 `@Column(name = "...")` 어노테이션을 명시적으로 사용합니다.
- **데이터 타입 최적화**: 거래량(`volume`), 주가(`price`), 기술적 지표 및 재무 비율 데이터는 오버플로우 방지와 고정밀 연산을 위해 `BigDecimal`(`DECIMAL(25, 4)`) 타입을 사용하여 금융 데이터의 정확성을 보장합니다.
- **Flyway 기반 스키마 관리**: 각 서비스의 `src/main/resources/db/migration`에 위치한 SQL 스크립트를 통해 DB 스키마 버전 관리를 수행합니다.

### 4. Spring Batch 운영 정책
- **자동 실행 방지**: 애플리케이션 시작 시 모든 Job이 자동으로 실행되는 것을 방지하기 위해 `spring.batch.job.enabled: false` 설정을 기본으로 합니다.
- **API 기반 트리거**: 배치는 각 서비스의 `/batch/**` 엔티티포인트를 통해 명시적으로 호출하거나 외부 스케줄러(Crontab 등)를 통해 실행합니다.

### 5. Phase 1: 데이터 무결성 및 전처리 (완료)
퀀트 분석의 신뢰도를 높이기 위해 원천 데이터 검증 및 가공 로직이 구축되었습니다.
- **데이터 타입 강화**: 주가, 거래량, 재무 항목 및 모든 기술적 지표 필드를 `BigDecimal`로 전환하여 고정밀 부동 소수점 연산 지원 및 오버플로우 원천 차단.
- **기업 이벤트 및 수정주가**: 증자, 분할 등 이벤트(`CorpEventHistory`)를 수집하고, 이를 반영한 수정종가(`adjClosePrice`) 자동 산출 로직 적용.
- **기술적 지표 엔진**: `ta4j` 라이브러리를 통합하여 이동평균선(MA), RSI, MACD, 볼린저밴드 등 핵심 지표를 `BigDecimal` 정밀도로 사전 계산.
- **재무 데이터 정합성 검증**: 수집된 재무제표의 대차대조표 등식(`자산=부채+자본`) 및 필수 필드 누락 여부를 즉시 검증하여 `ValidationStatus`(`VALID`/`INVALID`) 부여.

## 프로젝트 구조 (Multi-Module)

### 1. Root Project (`stock-msa`)
- 전체 프로젝트의 의존성 관리 및 전역 `.env` 관리.

### 2. Common Module (`modules/stock-common`)
- 공통 DTO (`CorpInfoDto`, `StockPriceDto`), 예외 처리, 유틸리티.
- **ValidationStatus**: 데이터 무결성 상태 관리를 위한 Enum.

### 3. Service Modules (`services/`)
- **stock-discovery**: Eureka Server. 모든 서비스의 등록 및 탐색.
- **stock-gateway**: 통합 라우팅 및 보안 필터링.
- **stock-corp**: 기업 기본 정보 및 테마/업종 관리.
- **stock-finance**: 재무제표 수집 및 **데이터 정합성 검증**.
- **stock-price**: 주가 시세, **수정주가 계산**, **기술적 지표 산출**.

## 실행 및 빌드 방법

### 사전 요구 사항
- Java 21 이상
- Docker & Docker Compose

### 빌드 및 실행 (Docker)
1. **환경 변수 설정**: 루트의 `.env.example`을 복사하여 `.env` 생성 및 값 수정.
2. **전체 기동**: `docker-compose up -d --build`
3. **상태 확인**: `docker-compose ps` (모든 서비스 `healthy` 확인)

### 로컬 실행 시 유의사항
- 로컬 DB 접속 시 `docker-compose.yaml`에 정의된 서비스별 계정(`corp_user`, `finance_user` 등)과 포트를 확인해야 합니다.
- 외부 API 키(`DATA_GO_SERVICE_KEY`, `DART_API_KEY`)가 `.env` 파일에 올바르게 설정되어 있어야 합니다.

---
*마지막 업데이트: 2026-02-05 (Phase 1 Complete)*