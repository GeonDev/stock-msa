# Stock-MSA (Microservice Architecture)

본 프로젝트는 공공데이터포털 및 DART API를 통해 기업 및 주식 데이터를 수집하고 분석하는 **MSA 기반의 주식 정보 시스템**입니다.

## 1. 프로젝트 주요 특징

- **서비스 탐색**: `stock-discovery`(Eureka)를 통한 마이크로서비스 동적 등록.
- **통합 게이트웨이**: `stock-gateway`를 통한 API 라우팅 및 보안 강화.
- **컨테이너화**: Docker Compose를 통한 인프라 및 전체 서비스의 원클릭 배포.
- **보안 강화**: 인프라 서비스별 독립 계정 관리 및 Actuator 엔드포인트 보호.

---

## 2. 개발 환경 준비

### 사전 요구 사항
- **Java 17** (Amazon Corretto 권장)
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
# 이미지 빌드 및 컨테이너 실행
docker-compose up -d --build
```

### 2) 서비스 포트 맵핑 정보
| 서비스 | 내부 포트 | 외부(호스트) 포트 | 용도 |
| :--- | :---: | :---: | :--- |
| **stock-gateway** | 8080 | **8080** | API Entry Point |
| **stock-discovery** | 8761 | **8761** | Eureka Dashboard |
| **stock-corp-db** | 3306 | **3306** | 기업 정보 DB |
| **stock-finance-db** | 3306 | **3307** | 재무 정보 DB |
| **stock-stock-db** | 3306 | **3308** | 주식 정보 DB |
| **stock-batch-db** | 3306 | **3309** | 배치 메타 DB (공유) |

### 3) 데이터 영속성 (Volumes)
각 데이터베이스는 Docker Volume을 통해 데이터가 영구 저장됩니다. 컨테이너를 내렸다 올려도 데이터는 유지됩니다.
- `corp-db-volume`: 기업 정보 DB 데이터
- `finance-db-volume`: 재무 정보 DB 데이터
- `stock-db-volume`: 주식 정보 DB 데이터
- `batch-db-volume`: 배치 메타 DB 데이터

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

---

## 5. API 보안

`stock-gateway` 및 인프라 서비스는 HTTP Basic 인증을 사용합니다.
- **Actuator 접근**: `.env`에 정의된 `GATEWAY_USER` 계정 정보 필요.
- **Eureka Dashboard**: `EUREKA_USER` 계정 정보 필요.

---

## 6. 주의 사항
- `.env` 파일은 절대 Git에 커밋하지 마세요 (중요 정보 포함).