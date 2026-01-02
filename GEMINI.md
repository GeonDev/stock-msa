# Gemini 프로젝트 분석: stock-batch

## 프로젝트 개요

이 프로젝트는 `stock-batch`라는 이름의 Spring Boot 기반 배치 애플리케이션입니다. 주요 목적은 외부 API(`data-go`)로부터 주식 및 기업 관련 데이터를 가져와 데이터베이스에 저장하는 것입니다. 이 애플리케이션은 REST API를 통해 트리거될 수 있는 특정 배치 작업을 실행하도록 설계되었습니다. Gradle 빌드 시스템을 사용하며 Java 17로 작성되었습니다.

## 주요 기술

-   **백엔드 프레임워크:** Spring Boot 3.4.8
-   **언어:** Java 17
-   **빌드 도구:** Gradle
-   **데이터 접근:** Spring Data JPA, Spring Batch, H2 Database (로컬/개발 프로필용)
-   **웹:** Spring Web, 내장 Undertow (Tomcat 대신)
-   **API 통신:** 비동기 API 호출을 위한 Spring WebFlux (`WebClient`)
-   **서비스 디스커버리:** Spring Cloud Netflix Eureka Client (비-로컬 프로필에서 활성화되도록 구성됨)
-   **설정 관리:** 외부화된 설정 관리를 위한 Spring Cloud Config
-   **로깅:** Tinylog
-   **유틸리티:** Lombok

## 프로젝트 구조

프로젝트는 표준 Maven/Gradle 레이아웃을 따릅니다:

-   `src/main/java`: Java 소스 코드를 포함합니다.
    -   `com.stock.batch`: 루트 패키지.
        -   `batchJob`: Spring Batch 작업 설정(`CorpInfoBatch`, `StockPriceBatch` 등)을 포함합니다.
            -   `ItemReader`: 배치 작업을 위한 사용자 정의 ItemReader로, 외부 API에서 데이터를 가져오는 역할을 합니다.
        -   `config`: 데이터베이스(`DataDBConfig`, `MetaDBConfig`), WebClient 등을 위한 Spring 설정 클래스.
        -   `controller`: 배치 작업을 수동으로 트리거하기 위한 REST 컨트롤러(`BatchController`).
        -   `entity`: 데이터 모델을 나타내는 JPA 엔티티(예: `CorpInfo`, `StockPrice`).
        -   `enums`: 애플리케이션 전반에 사용되는 열거형(예: `StockMarket`, `CorpCurrency`).
        -   `repository`: 데이터베이스 작업을 위한 Spring Data JPA 리포지토리.
        -   `service`: `StockApiService`를 포함한 비즈니스 로직으로, 외부 API와 통신합니다.
-   `src/main/resources`: 애플리케이션 리소스.
    -   `application.yaml`: Spring Boot 설정 파일 (예: `dev`, `local`과 같은 프로필 포함).
-   `build.gradle`: 의존성, 플러그인 및 빌드 설정을 정의하는 Gradle 빌드 스크립트.

## 아키텍처 및 워크플로우

1.  **외부 설정:** 시작 시 애플리케이션은 Spring Cloud Config 서버에 연결하여 데이터베이스 자격 증명 및 API 키를 포함한 설정을 가져옵니다. 이는 `spring-cloud-starter-config` 의존성과 `StockBatchApplication.java`에 설정된 동적 속성에서 알 수 있습니다.

2.  **데이터베이스 설정:** 애플리케이션은 `application.yaml`에 정의된 대로 두 개의 별도 H2 데이터베이스(`datasource-meta` 및 `datasource-data`)에 연결하도록 구성됩니다. Spring Batch 메타데이터 테이블의 스키마는 시작 시 초기화됩니다.

3.  **API 트리거 배치 작업:** `BatchController`는 여러 REST 엔드포인트(예: `/batch/price`, `/batch/corp-info`)를 노출합니다. 이러한 엔드포인트 중 하나로 HTTP POST 요청이 전송되면 해당 Spring Batch 작업이 트리거됩니다.

4.  **배치 처리:**
    -   각 배치 작업(`CorpInfoBatch`와 같은)은 Spring Bean으로 정의되며 하나 이상의 `Step`으로 구성됩니다.
    -   일반적인 단계는 `Reader`, `Processor`, `Writer`로 구성됩니다.
        -   **Reader (`ItemReader`):** 사용자 정의 `ItemReader`(예: `CorpInfoItemReader`)는 `StockApiService`를 호출합니다.
        -   **Service (`StockApiService`):** 이 서비스는 `WebClient`를 사용하여 `data-go` API에 HTTP 요청을 하여 원시 데이터를 가져옵니다.
        -   **Processor (`ItemProcessor`):** Reader가 가져온 데이터를 처리합니다. 제공된 `CorpInfoBatch`에서 프로세서는 간단한 통과(pass-through)입니다.
        -   **Writer (`ItemWriter`):** `RepositoryItemWriter`는 처리된 항목(JPA 엔티티)을 Spring Data 리포지토리(예: `CorpInfoRepository`)를 통해 청크 단위로 데이터베이스에 저장하는 데 사용됩니다.

5.  **서비스 디스커버리:** 비-로컬 프로필의 경우, 애플리케이션은 Eureka 서비스 레지스트리에 자신을 등록하여 마이크로서비스 아키텍처의 다른 서비스에 의해 발견될 수 있도록 합니다.

## 실행 방법

1.  **전제 조건:**
    -   Java 17
    -   `http://localhost:9000`에서 실행 중인 Spring Cloud Config 서버 (또는 환경 변수를 통해 구성된 대로).
    -   설정 서버는 데이터소스 및 `data-go.service-key`에 대한 속성을 제공해야 합니다.
    -   (선택 사항, 비-로컬 프로필용) 실행 중인 Eureka 서버.

2.  **애플리케이션 빌드:**
    ```bash
    ./gradlew build
    ```

3.  **애플리케이션 실행:**
    ```bash
    java -jar build/libs/stock-batch-0.0.1-SNAPSHOT.jar
    ```

4.  **배치 작업 트리거:**
    -   주식 가격을 가져오려면:
        ```bash
        curl -X POST "http://localhost:8085/batch/price?market=KOSPI"
        ```
    -   기업 정보를 가져오려면:
        ```bash
        curl -X POST "http://localhost:8085/batch/corp-info"
        ```

## 요약

이것은 외부 소스에서 데이터를 수집하기 위해 설계된 잘 구성된 Spring Batch 애플리케이션입니다. 외부 설정 및 서비스 디스커버리를 활용하는 클라우드 네이티브 애플리케이션입니다. 작업을 트리거하기 위해 REST 엔드포인트를 사용하는 것은 데이터 동기화 작업을 관리하는 유연한 방법을 제공합니다. 코드는 모듈화되어 있으며, 작업 정의, 데이터 접근 및 API 통신과 같은 관심사를 분리합니다.