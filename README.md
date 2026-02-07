# Stock-MSA (Microservice Architecture)

본 프로젝트는 공공데이터포털 및 DART API를 통해 기업 및 주식 데이터를 수집하고 분석하여, **동적 자산배분(퀀트) 투자**에 도움을 주기 위한 **MSA 기반의 주식 정보 시스템**입니다.

## 1. 프로젝트 주요 특징

- **서비스 탐색**: `stock-discovery`(Eureka)를 통한 마이크로서비스 동적 등록.
- **통합 게이트웨이**: `stock-gateway`를 통한 API 라우팅 및 보안 강화.
- **컨테이너화**: Docker Compose를 통한 인프라 및 전체 서비스의 원클릭 배포.
- **보안 강화**: 인프라 서비스별 독립 계정 관리 및 Actuator 엔드포인트 보호.
- **서비스 간 통신**: RestClient 기반의 안정적인 마이크로서비스 간 데이터 연동.
- **데이터 무결성 (Phase 1 완료)**:
    - **정합성 검증**: 재무제표의 대차대조표 등식 및 필수 값 검증 로직 탑재.
    - **퀀트 분석 준비**: 수정주가(Split/Dividend Adjusted) 자동 계산 및 `Ta4j` 기반 기술적 지표(RSI, MACD, Bollinger Bands, **Momentum**) 사전 적재.
    - **재무 지표 계산**: PER, PBR, ROE, ROA 등 주요 재무 지표 자동 산출 (93% 성공률).
    - **배치 안정성**: Chunk 기반의 표준화된 배치 아키텍처 적용 및 `ApplicationConstants`를 통한 처리 성능 중앙 제어.

---

## 2. 개발 환경 준비

### 사전 요구 사항
- **Java 21** (Amazon Corretto 권장)
- **Docker** & **Docker Compose**

### 환경 변수 설정 (`.env`)
프로젝트 루트에 `.env` 파일을 생성하고 아래 내용을 참고하여 작성하세요.
```properties
# API Keys
DATA_GO_SERVICE_KEY=your_key
DART_API_KEY=your_key

# Security Credentials
EUREKA_USER=eurekaAdmin
EUREKA_PASSWORD=your_password
GATEWAY_USER=gatewayAdmin
GATEWAY_PASSWORD=your_password
```

---

## 3. 실행 가이드 (Docker Compose)

### 1) 서비스 전체 기동
```bash
# 로컬 빌드 없이 바로 실행 (Multi-stage Dockerfile)
docker-compose up -d --build
```

**주요 개선사항:**
- ✅ 로컬 Gradle 빌드 불필요
- ✅ Docker 내부에서 자동 빌드
- ✅ 빌드 캐시 활용으로 재빌드 시간 단축

### 2) 서비스 포트 맵핑 정보
| 서비스 | 내부 포트 | 외부(호스트) 포트 | 용도 |
| :--- | :---: | :---: | :--- |
| **stock-gateway** | 8080 | **8080** | API Entry Point |
| **stock-discovery** | 8761 | **8761** | Eureka Dashboard |
| **stock-corp** | 8081 | **8081** | 기업 정보 서비스 |
| **stock-finance** | 8082 | **8082** | 재무 정보 서비스 |
| **stock-price** | 8083 | **8083** | 주식 정보 서비스 |
| **stock-strategy** | 8084 | **8084** | 백테스팅 전략 서비스 |
| **stock-corp-db** | 3306 | **3306** | 기업 정보 DB |
| **stock-finance-db** | 3306 | **3307** | 재무 정보 DB |
| **stock-price-db** | 3306 | **3308** | 주식 정보 DB |
| **stock-batch-db** | 3306 | **3309** | 배치 메타 DB (공유) |
| **stock-strategy-db** | 3306 | **3310** | 백테스팅 전략 DB |

### 3) 데이터 영속성 (Volumes)
각 데이터베이스는 Docker Volume을 통해 데이터가 영구 저장됩니다. 컨테이너를 내렸다 올려도 데이터는 유지됩니다.
- `corp-db-volume`: 기업 정보 DB 데이터
- `finance-db-volume`: 재무 정보 DB 데이터
- `price-db-volume`: 주식 정보 DB 데이터
- `batch-db-volume`: 배치 메타 DB 데이터
- `strategy-db-volume`: 백테스팅 전략 DB 데이터

---

## 4. 로컬 개발 및 테스트

로컬에서 개별 서비스를 실행할 때는 `local` 프로필을 사용하세요. 이 프로필은 Discovery를 비활성화하여 외부 의존성 없이 기동할 수 있게 도와줍니다.

```bash
# IntelliJ 등 IDE에서 실행 시 VM Options
-Dspring.profiles.active=local
```

### Gateway 라우팅 규칙
- 기업 정보: `/api/v1/corp/**`
- 재무 정보: `/api/v1/finance/**`
- 주식 정보: `/api/v1/stock/**`
- 백테스팅 전략: `/api/v1/strategy/**`

---

## 5. API 보안

`stock-gateway` 및 인프라 서비스는 HTTP Basic 인증을 사용합니다.
- **Actuator 접근**: `.env`에 정의된 `GATEWAY_USER` 계정 정보 필요.
- **Eureka Dashboard**: `EUREKA_USER` 계정 정보 필요.

---

## 6. 주요 기술 스택

### 핵심 기술
- **Java 21** (Amazon Corretto)
- **Spring Boot 3.4.8** / **Spring Cloud 2024.0.2**
- **Spring Batch**: 대용량 데이터 수집 및 처리
- **Spring Data JPA** + **Flyway**: 데이터베이스 관리
- **MySQL 8.0**: 도메인별 독립 데이터베이스

### 주요 라이브러리
- **Jackson JSR310**: Java 8 날짜/시간 타입 직렬화 (JavaTimeModule)
- **Ta4j**: 기술적 지표 계산 (RSI, MACD, Bollinger Bands)
- **RestClient**: 마이크로서비스 간 통신
- **Undertow**: 고성능 웹 서버
- **Lombok**: 보일러플레이트 코드 감소

### 배치 아키텍처
- **ItemReader 분리**: 모든 배치 Reader는 `batchJob/ItemReader/` 패키지에 독립 클래스로 구성
- **Chunk 기반 처리**: 대용량 데이터를 청크 단위로 안정적 처리
- **트랜잭션 관리**: 청크별 트랜잭션으로 장애 격리

---

## 7. 연동 외부 API 정보

본 시스템은 공신력 있는 데이터 확보를 위해 아래의 외부 API를 연동하고 있습니다.

### 1) 공공데이터포털 (금융위원회 및 유관기관)
- **주식시세정보**: KRX 상장 종목의 일별 시세 데이터.
- **기업 재무 정보**: 상장 기업의 요약 재무상태표 및 손익계산서.
- **상장종목정보**: KRX 상장 종목 리스트 및 종목 기본 정보.
- **특일 정보 조회**: 주식 시장 휴장일 판단을 위한 공휴일 데이터.

### 2) Open DART (금융감독원)
- **공시 정보 및 재무 사항**: 기업별 주요 공시 및 상세 재무제표(단일/다중 회사) 데이터.

---

## 8. 주의 사항
- `.env` 파일은 절대 Git에 커밋하지 마세요 (중요 정보 포함).
- **Stock Code 형식**: 기업 정보는 `A900100` (A 접두사), 주가 데이터는 `900100` (숫자만) 형식을 사용합니다.
- **서비스 간 통신**: Docker 환경에서는 서비스 이름(예: `stock-price:8083`)을 사용하고, 로컬 개발 시에는 `localhost`를 사용합니다.