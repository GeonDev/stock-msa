# Gemini Project Analysis: stock-batch

## Project Overview

This project, named `stock-batch`, is a Spring Boot-based batch application. Its primary purpose is to fetch stock and corporate data from an external API (`data.go.kr`) and store it in a database. The application is designed to run specific batch jobs that can be triggered via a REST API. It uses the Gradle build system and is written in Java 17.

## Key Technologies

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 17
- **Build Tool:** Gradle
- **Data Access:** Spring Data JPA, Spring Batch, H2 Database (for local/dev profiles)
- **Web:** Spring Web, embedded Undertow (in place of Tomcat)
- **API Communication:** Spring's `RestClient` for synchronous API calls.
- **Service Discovery:** Spring Cloud Netflix Eureka Client (configured to be active in non-local profiles)
- **Configuration Management:** Spring Cloud Config for externalized configuration management.
- **Logging:** Tinylog
- **Database Migration:** Flyway
- **Utilities:** Lombok, dotenv-java

## Project Structure

The project follows a standard Maven/Gradle layout, with specialized packages for different domains:

- `src/main/java`: Contains the Java source code.
    - `com.stock.batch`: The root package.
        - `corp`: Corporate information domain.
            - `batchJob`: Batch job configurations (`CorpInfoBatch`, `CorpDetailBatch`).
            - `entity`: JPA entities (`CorpInfo`, `CorpDetail`).
            - `repository`: Repositories for corporate data.
            - `controller`: `CorpInfoController` for triggering corporate-related batches.
        - `finance`: Financial data domain (`CorpFinance`, indicators).
        - `stock`: Stock price and indicator domain.
        - `global`: Shared configurations, enums, exceptions, and utilities.
- `src/main/resources`: Application resources.
    - `application.yaml`: The main Spring Boot configuration file.
    - `db/migration`: Flyway database migration scripts.
- `build.gradle`: The Gradle build script defining dependencies, plugins, and build settings.
- `Dockerfile`: For containerizing the application.

## Architecture and Workflow

1. **Configuration:** On startup, the application's profile determines its configuration strategy (Local `.env` vs. Cloud Config).

2. **Database Setup:** Uses Flyway for schema management. Supports multiple DB configurations (Meta/Data DB).

3. **Batch Processing:**
    - **CorpInfoBatch**: Fetches master data for listed companies from the external API and saves it to `TB_CORP_INFO`.
    - **CorpDetailBatch (Chunk-oriented)**: 
        - Reads from `TB_CORP_INFO`.
        - **Data Enrichment**: Parses `isinCode` (prefix) to determine `CorpNational`.
        - **State Management**: Compares `checkDt` with the current date to set `state` as `ACTIVE` (if updated today) or `DEL` (if not refreshed).
        - Saves results to `TB_CORP_DETAIL`.
    - **StockPriceBatch**: Fetches daily stock prices and calculates indicators.

4. **API Endpoints**:
    - `POST /batch/corp-info`: Triggers company master data collection.
    - `POST /batch/corp-detail/cleanup`: Triggers company state and detail synchronization.
    - `POST /batch/price`: Triggers stock price collection for a specific market.

## How to Run

### Prerequisites
- Java 17
- Docker

### Building the Application
```bash
./gradlew build
```

### Running with the `local` profile
1. Create `.env` from `.env.example`.
2. Start H2 Database.
3. Run the jar with `spring.profiles.active=local`.

### Triggering Batch Jobs
- **Fetch Master Info**:
  ```bash
  curl -X POST "http://localhost:8085/batch/corp-info"
  ```
- **Cleanup and Update Details**:
  ```bash
  curl -X POST "http://localhost:8085/batch/corp-detail/cleanup"
  ```