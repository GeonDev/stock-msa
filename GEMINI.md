# Gemini Project Analysis: stock-msa

## 프로젝트 개요

`stock-msa`는 단일 배치 서비스 구조에서 도메인 중심의 **MSA(Microservice Architecture)** 환경으로 리팩토링된 프로젝트입니다. **Gradle Multi-Module Monorepo** 구조를 취하고 있으며, **Docker 기반의 통합 개발 환경**과 **중앙 집중식 설정 관리**를 제공합니다.

## 주요 기술 스택

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 17 (Amazon Corretto)
- **Build Tool:** Gradle (Multi-Module)
- **Microservices Support:**
  - **Service Discovery:** Spring Cloud Netflix Eureka (Port: 8761)
  - **API Gateway:** Spring Cloud Gateway (**Reactive/WebFlux**) (Port: 8080)
  - **Config Management:** Spring Cloud Config (Port: 8888)
- **Security:** Spring Security (HTTP Basic Auth, 서비스별 계정 분리)
- **Data Access:** Spring Data JPA, Spring Batch, MySQL
- **Containerization:** Docker, Docker Compose (Healthcheck 적용)
- **Utilities:** Tinylog, Lombok, dotenv-java

## 주요 아키텍처 및 특징

### 1. 하이브리드 설정 관리 (Hybrid Configuration)
- **Local Profile**: `active: local` 시 Discovery 및 Config Client를 비활성화하여 독립적인 개발 및 테스트가 가능합니다.
- **Prod Profile**: Docker 환경에서는 Discovery 및 Config Server를 통해 동적으로 설정을 로드합니다.

### 2. 보안 및 인증 (Security)
- **서비스별 독립 계정**: `CONFIG_SERVER_USER`, `EUREKA_USER`, `GATEWAY_USER`를 통해 각 인프라 서비스의 접근 권한을 분리했습니다.
- **Reactive Security**: Gateway는 WebFlux 기반의 `SecurityWebFilterChain`을 사용하여 비동기 논블로킹 환경에 최적화된 보안을 제공합니다.

### 3. Docker 통합 환경
- `amazoncorretto:17-alpine` 이미지를 사용하여 Apple Silicon(M1/M2) 및 일반 x86 환경 모두를 지원합니다.
- `depends_on` 및 `healthcheck`를 통해 서비스 간 기동 의존성(Discovery -> Config -> Apps)을 보장합니다.

### 4. 공유 배치 데이터베이스 (Shared Batch Database)
- **메타데이터 분리**: 각 도메인 서비스(`stock-corp`, `stock-finance`, `stock-stock`)는 비즈니스 데이터용 독립 DB 외에 **공유된 배치 DB(`stock_batch`)** 를 사용합니다.
- **Spring Batch 통합**: `@BatchDataSource`를 통해 Spring Batch 메타데이터(Job, Step Execution 등)를 중앙에서 통합 관리합니다.

## 프로젝트 구조 (Multi-Module)

### 1. Root Project (`stock-msa`)
- 전체 프로젝트의 의존성 관리 및 전역 `.env` 관리.

### 2. Common Module (`modules/stock-common`)
- 공통 DTO (`CorpInfoDto`, `StockPriceDto`), 예외 처리, 유틸리티.

### 3. Service Modules (`services/`)
- **stock-discovery**: Eureka Server. 모든 서비스의 등록 및 탐색.
- **stock-config**: Config Server. Git 저장소 기반의 중앙 설정 관리.
- **stock-gateway**: 통합 라우팅 및 보안 필터링.
- **stock-corp/finance/stock**: 독립된 DB를 가진 도메인 마이크로서비스.

## 실행 및 빌드 방법

### 사전 요구 사항
- Java 17 이상
- Docker & Docker Compose

### 빌드 및 실행 (Docker)
1. **환경 변수 설정**: 루트의 `.env.example`을 복사하여 `.env` 생성 및 값 수정.
2. **전체 기동**: `docker-compose up -d --build`
3. **상태 확인**: `docker-compose ps` (모든 서비스 `healthy` 확인)

---
*마지막 업데이트: 2026-02-03*
