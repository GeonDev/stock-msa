# Gemini Project Analysis: stock-msa

## 프로젝트 개요

`stock-msa`는 기존 단일 프로젝트(`stock-batch`)를 **Gradle Multi-Module Monorepo** 구조로 리팩토링한 프로젝트입니다. 이 프로젝트의 핵심 목적은 공공데이터포털(`data.go.kr`)로부터 주식 및 기업 정보를 수집하여 데이터베이스에 저장하고, 이를 MSA(Microservice Architecture) 환경에서 효율적으로 관리하고 제공하는 것입니다.

## 주요 기술 스택

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 17
- **Build Tool:** Gradle (Multi-Module)
- **Microservices Support:**
  - **Service Discovery:** Spring Cloud Netflix Eureka
  - **API Gateway:** Spring Cloud Gateway
  - **Config Management:** Spring Cloud Config
- **Data Access:** Spring Data JPA, Spring Batch, MySQL (Local/Dev)
- **Web:** Spring Web, Undertow (Embedded Container)
- **Communication:** Spring RestClient
- **Database Migration:** Flyway
- **Logging:** Tinylog
- **Utilities:** Lombok, dotenv-java

## 프로젝트 구조 (Multi-Module)

프로젝트는 공통 모듈과 개별 서비스 모듈로 구성됩니다.

### 1. Root Project (`stock-msa`)
- 전체 프로젝트의 의존성 관리 및 서브 모듈 설정을 담당합니다.

### 2. Common Module (`modules/stock-common`)
- 여러 서비스에서 재사용되는 공통 코드를 포함합니다.
- **주요 구성:**
  - `config`: 공통 빈 설정 (RestClient, EnumConverter 등)
  - `enums`: 공용 열거형 (CorpNational, StockMarket, CorpState 등)
  - `exception`: 전역 예외 처리 (GlobalExceptionHandler, CustomApiException)
  - `utils`: 공통 유틸리티

### 3. Service Modules (`services/`)
- **stock-batch**: 주식/기업 데이터 수집 및 처리 담당.
  - 외부 API 연동 및 데이터베이스 저장 로직 포함.
- **stock-discovery**: Eureka Server를 이용한 서비스 등록 및 탐색.
- **stock-gateway**: Spring Cloud Gateway를 이용한 API 라우팅 및 보안.
- **stock-config**: 중앙 집중식 설정 관리를 위한 Config Server.

## 아키텍처 및 작업 흐름

1. **설정 관리**: 실행 프로필(Local, Dev, Prod)에 따라 로컬 `.env` 또는 `stock-config` 서버를 통해 설정을 로드합니다.
2. **서비스 등록**: 각 마이크로서비스는 기동 시 `stock-discovery`(Eureka) 서버에 등록됩니다.
3. **데이터 수집 (Batch Processing)**:
    - **CorpInfoBatch**: 상장 기업 마스터 데이터를 외부 API에서 가져와 `TB_CORP_INFO`에 저장합니다.
    - **CorpDetailBatch**: `TB_CORP_INFO`를 읽어 기업 상세 정보를 고도화하고, `checkDt`를 기준으로 `ACTIVE` 또는 `DEL` 상태를 관리하여 `TB_CORP_DETAIL`에 저장합니다.
    - **StockPriceBatch**: 일별 주식 시세 정보를 수집하고 관련 지표를 계산합니다.
4. **API 라우팅**: 모든 외부 요청은 `stock-gateway`를 거쳐 적절한 서비스로 전달됩니다.

## 실행 및 빌드 방법

### 사전 요구 사항
- Java 17 이상
- Docker

### 빌드
```bash
./gradlew clean build
```

### 실행 (로컬 환경)
1. `services/stock-batch`의 `.env.example`을 참고하여 `.env` 파일을 생성합니다.
2. Infra 서비스(Discovery, Config)를 먼저 실행한 후 개별 서비스(Batch, Gateway)를 실행합니다.
3. 실행 시 `-Dspring.profiles.active=local` 옵션을 사용합니다.

### 배치 작업 트리거 (예시)
- **기업 마스터 정보 수집**:
  ```bash
  curl -X POST "http://localhost:8085/batch/corp-info"
  ```
- **기업 상태 정리 및 상세 정보 갱신**:
  ```bash
  curl -X POST "http://localhost:8085/batch/corp-detail/cleanup"
  ```
