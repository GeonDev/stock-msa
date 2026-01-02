# Gemini Project Analysis: stock-batch

## Project Overview

This project is a Spring Boot-based batch application named `stock-batch`. Its primary purpose is to fetch stock and corporate-related data from an external API (`data-go`) and store it in a database. The application is designed to run specific batch jobs that can be triggered via a REST API. It uses a Gradle build system and is written in Java 17.

## Key Technologies

- **Backend Framework:** Spring Boot 3.4.8
- **Language:** Java 17
- **Build Tool:** Gradle
- **Data Access:** Spring Data JPA, Spring Batch, H2 Database (for local/dev profiles)
- **Web:** Spring Web, embedded Undertow (instead of Tomcat)
- **API Communication:** Spring WebFlux (`WebClient`) for asynchronous API calls.
- **Service Discovery:** Spring Cloud Netflix Eureka Client (configured to be active on non-local profiles).
- **Configuration:** Spring Cloud Config for externalized configuration management.
- **Logging:** Tinylog
- **Utilities:** Lombok

## Project Structure

The project follows a standard Maven/Gradle layout:

- `src/main/java`: Contains the Java source code.
    - `com.stock.batch`: The root package.
        - `batchJob`: Contains the Spring Batch job configurations (`CorpInfoBatch`, `StockPriceBatch`, etc.).
            - `ItemReader`: Custom ItemReaders for the batch jobs, responsible for fetching data from the external API.
        - `config`: Spring configuration classes for databases (`DataDBConfig`, `MetaDBConfig`), WebClient, etc.
        - `controller`: REST controllers (`BatchController`) to trigger the batch jobs manually.
        - `entity`: JPA entities representing the data model (e.g., `CorpInfo`, `StockPrice`).
        - `enums`: Enumerations used throughout the application (e.g., `StockMarket`, `CorpCurrency`).
        - `repository`: Spring Data JPA repositories for database operations.
        - `service`: Business logic, including the `StockApiService` which communicates with the external API.
- `src/main/resources`: Application resources.
    - `application.yaml`: Spring Boot configuration file, with profiles for different environments (e.g., `dev`, `local`).
- `build.gradle`: The Gradle build script defining dependencies, plugins, and build settings.

## Architecture and Workflow

1.  **External Configuration:** On startup, the application connects to a Spring Cloud Config server to fetch its configuration, including database credentials and API keys. This is evident from the `spring-cloud-starter-config` dependency and the dynamic properties set in `StockBatchApplication.java`.

2.  **Database Setup:** The application is configured to connect to two separate H2 databases, as defined in `application.yaml` (`datasource-meta` and `datasource-data`). The schema for the Spring Batch metadata tables is initialized on startup.

3.  **API Triggered Batch Jobs:** The `BatchController` exposes several REST endpoints (e.g., `/batch/price`, `/batch/corp-info`). When an HTTP POST request is sent to one of these endpoints, it triggers a corresponding Spring Batch job.

4.  **Batch Processing:**
    - Each batch job (like `CorpInfoBatch`) is defined as a Spring Bean and consists of at least one `Step`.
    - A typical step is composed of a `Reader`, a `Processor`, and a `Writer`.
        - **Reader (`ItemReader`):** A custom `ItemReader` (e.g., `CorpInfoItemReader`) calls the `StockApiService`.
        - **Service (`StockApiService`):** This service uses `WebClient` to make an HTTP request to the `data-go` API to fetch raw data.
        - **Processor (`ItemProcessor`):** Processes the data fetched by the reader. In the provided `CorpInfoBatch`, the processor is a simple pass-through.
        - **Writer (`ItemWriter`):** A `RepositoryItemWriter` is used to save the processed items (JPA entities) into the database in chunks via a Spring Data repository (e.g., `CorpInfoRepository`).

5.  **Service Discovery:** For non-local profiles, the application registers itself with a Eureka service registry, allowing it to be discovered by other services in a microservices architecture.

## How to Run

1.  **Prerequisites:**
    - Java 17
    - A running Spring Cloud Config server at `http://localhost:9000` (or as configured via environment variables).
    - The config server must provide properties for datasources and the `data-go.service-key`.
    - (Optional, for non-local profiles) A running Eureka server.

2.  **Build the application:**
    ```bash
    ./gradlew build
    ```

3.  **Run the application:**
    ```bash
    java -jar build/libs/stock-batch-0.0.1-SNAPSHOT.jar
    ```

4.  **Trigger a batch job:**
    - To fetch stock prices:
      ```bash
      curl -X POST "http://localhost:8085/batch/price?market=KOSPI"
      ```
    - To fetch corporate information:
      ```bash
      curl -X POST "http://localhost:8085/batch/corp-info"
      ```

## Summary

This is a well-structured Spring Batch application designed for data ingestion from an external source. It is cloud-native, leveraging external configuration and service discovery. The use of REST endpoints to trigger jobs provides a flexible way to manage data synchronization tasks. The code is modular, separating concerns like job definition, data access, and API communication.
