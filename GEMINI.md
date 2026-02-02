# Gemini Project Analysis: stock-msa

## 프로젝트 개요

`stock-msa`는 단일 배치 서비스 구조에서 도메인 중심의 **MSA(Microservice Architecture)** 환경으로 리팩토링된 프로젝트입니다. **Gradle Multi-Module Monorepo** 구조를 취하고 있으며, 공공데이터포털(`data.go.kr`)로부터 수집한 주식 및 기업 정보를 도메인별 독립된 데이터베이스에 저장하고 제공합니다.

## 주요 기술 스택

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 17
- **Build Tool:** Gradle (Multi-Module)
- **Microservices Support:**
  - **Service Discovery:** Spring Cloud Netflix Eureka (Port: 8761)
  - **API Gateway:** Spring Cloud Gateway (Port: 8080)
  - **Config Management:** Spring Cloud Config (Port: 9000)
- **Data Access:** Spring Data JPA, Spring Batch, MySQL (Domain-specific DBs)
- **Web:** Spring Web, Undertow (Embedded Container)
- **Communication:** Spring RestClient (Synchronous DTO-based communication)
- **Database Migration:** Flyway (Per-service migration)
- **Logging:** Tinylog
- **Utilities:** Lombok, dotenv-java

## 프로젝트 구조 (Multi-Module)

프로젝트는 도메인 중립적인 공통 모듈과 독립적인 마이크로서비스들로 구성됩니다.

### 1. Root Project (`stock-msa`)
- 전체 프로젝트의 의존성 관리 및 전역 설정을 담당합니다.

### 2. Common Module (`modules/stock-common`)
- **주요 구성:**
  - `config`: 공통 빈 설정 (RestClient, EnumConverter 등)
  - `dto`: 서비스 간 통신을 위한 공용 DTO (`CorpInfoDto`, `StockPriceDto`)
  - `enums`: 도메인 공용 열거형
  - `exception`: 전역 예외 처리 로직
  - `utils`: 날짜 및 파싱 유틸리티

### 3. Service Modules (`services/`)
- **stock-corp (Port: 8081)**: 기업 마스터 및 상세 정보 관리 도메인.
- **stock-finance (Port: 8082)**: 기업 재무 제표 및 관련 지표 계산 도메인.
- **stock-stock (Port: 8083)**: 주식 시세, 지표 및 시장 정보 관리 도메인.
- **stock-discovery**: Eureka Server를 이용한 서비스 등록 및 탐색.
- **stock-gateway**: 모든 API 요청의 통합 라우팅(Service ID 기반) 및 보안.
- **stock-config**: 전역 공통 설정(Native Profile) 제공.

## 아키텍처 및 작업 흐름

1. **설정 및 등록**: 각 서비스는 기동 시 로컬 `application.yaml`의 DB/Eureka 정보를 로드하며, Eureka에 등록됩니다.
2. **데이터 수집 (Batch Processing)**:
    - 각 도메인 서비스(`corp`, `finance`, `stock`) 내부에 관련 배치 작업이 포함되어 있습니다.
    - **통신 흐름**: 예를 들어, `stock-finance` 배치는 지표 계산을 위해 `stock-corp` 및 `stock-stock` 서비스의 내부 API를 DTO 기반으로 호출하여 필요한 데이터를 획득합니다.
3. **데이터베이스 분리**: 각 서비스는 Docker Compose로 구성된 독립된 MySQL 인스턴스(또는 스키마)를 사용하며, Flyway를 통해 개별적으로 스키마 버전을 관리합니다.
4. **API 라우팅**: 외부 클라이언트는 `stock-gateway`를 통해 `/api/v1/{domain}/**` 경로로 각 도메인 서비스에 접근합니다.

## 실행 및 빌드 방법

### 사전 요구 사항
- Java 17 이상
- Docker (도메인별 DB 실행 필수)

### 빌드
```bash
./gradlew clean build
```

### 실행 (로컬 환경)
1. **인프라 기동**: Docker를 통해 도메인별 DB를 실행합니다.
   ```bash
   docker-compose up -d
   ```
2. **서비스 실행**: `discovery`, `config` 서버를 먼저 실행한 후, 개별 도메인 서비스와 `gateway`를 기동합니다.
   - 각 서비스의 `.env` 파일(Data.go.kr API Key 포함)이 필요합니다.

### API 및 배치 테스트
- **Gateway를 통한 기업 정보 조회**:
  ```bash
  curl http://localhost:8080/api/v1/corp/internal/{corpCode}
  ```
- **재무 데이터 수집 배치 실행**:
  ```bash
  curl -X POST "http://localhost:8082/batch/corp-finance?date=20240101"
  ```