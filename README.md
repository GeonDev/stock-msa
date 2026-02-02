# Stock-MSA (Microservice Architecture)

본 프로젝트는 공공데이터포털 및 DART API를 통해 기업 및 주식 데이터를 수집하고 분석하는 **MSA 기반의 주식 정보 시스템**입니다. 기존 단일 배치 프로젝트에서 도메인별 독립 서비스 구조로 리팩토링되었습니다.

## 1. 프로젝트 구조

- **modules/stock-common**: 공통 Entity, DTO, 유틸리티 및 예외 처리.
- **services/stock-discovery**: 서비스 등록 및 탐색 (Eureka Server).
- **services/stock-gateway**: 통합 API 진입점 및 라우팅.
- **services/stock-config**: 중앙 집중식 설정 관리.
- **services/stock-corp**: 기업 정보 도메인 (Master/Detail).
- **services/stock-finance**: 기업 재무 제표 및 지표 도메인.
- **services/stock-stock**: 주가 시세 및 기술적 지표 도메인.

---

## 2. 개발 환경 준비

### 사전 요구 사항
- **Java 17**
- **Docker** (데이터베이스 및 인프라 실행용)

### API 키 설정 (`.env`)
프로젝트 루트에 `.env` 파일을 생성하고 아래 키 정보를 입력하세요.
```properties
data-go.service-key=YOUR_SERVICE_KEY
dart.api-key=YOUR_API_KEY
```

---

## 3. 데이터베이스 및 인프라 설정 (Docker)

본 프로젝트는 도메인 격리를 위해 **3개의 독립된 MySQL 인스턴스**를 사용합니다. 루트 디렉토리의 `docker-compose.yaml`을 통해 한 번에 실행할 수 있습니다.

### 1) 인프라 실행
```bash
docker-compose up -d
```

### 2) 데이터베이스 정보
| 서비스 | 외부 포트 | 데이터베이스 명 | 사용자/암호 |
| :--- | :---: | :--- | :--- |
| stock-corp | 3306 | `stock_corp` | user / password |
| stock-finance | 3307 | `stock_finance` | user / password |
| stock-stock | 3308 | `stock_stock` | user / password |

*참고: 각 서비스 실행 시 Flyway가 자동으로 최신 스키마(`V1__init_*.sql`)를 적용합니다.*

---

## 4. 빌드 및 실행

### 전체 프로젝트 빌드
```bash
./gradlew clean build
```

### 서비스 실행 순서
서비스 간 의존성을 위해 아래 순서대로 실행하는 것을 권장합니다.

1.  **stock-discovery** (Port: 8761)
2.  **stock-config** (Port: 9000)
3.  **Domain Services** (corp: 8081, finance: 8082, stock: 8083)
4.  **stock-gateway** (Port: 8080)

```bash
# 예시: 개별 서비스 실행
java -jar services/stock-corp/build/libs/stock-corp-0.0.1-SNAPSHOT.jar
```

---

## 5. API 사용 및 라우팅

모든 외부 요청은 **Gateway(8080)**를 통해 전달됩니다.

- **Gateway 주소**: `http://localhost:8080`
- **라우팅 규칙**:
    - 기업 정보: `/api/v1/corp/**` -> `stock-corp`
    - 재무 정보: `/api/v1/finance/**` -> `stock-finance`
    - 주식 정보: `/api/v1/stock/**` -> `stock-stock`

### 주요 API 예시
- **특정 기업 조회**: `GET http://localhost:8080/api/v1/corp/internal/{corpCode}`
- **최신 주가 조회**: `GET http://localhost:8080/api/v1/stock/internal/price/latest/{stockCode}`
- **재무 배치 실행**: `POST http://localhost:8080/api/v1/finance/batch?date=20240101` (예정)

---

## 6. 주의 사항
- 각 서비스의 `application.yaml`에는 로컬 개발을 위한 기본 설정이 포함되어 있습니다.
- 서비스 간 통신은 `stock-common`에 정의된 **DTO**를 통해 이루어지며, 엔티티를 직접 노출하지 않습니다.