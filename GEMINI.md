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

The project follows a standard Maven/Gradle layout:

- `src/main/java`: Contains the Java source code.
    - `com.stock.batch`: The root package.
        - `batchJob`: Contains the Spring Batch job configurations (`CorpInfoBatch`, `StockPriceBatch`, etc.).
            - `ItemReader`: Custom ItemReaders for the batch jobs, responsible for fetching data from the external API.
        - `config`: Spring configuration classes for the database, `RestClient`, etc.
        - `controller`: The REST controller (`BatchController`) for manually triggering batch jobs.
        - `entity`: JPA entities representing the data model (e.g., `CorpInfo`, `StockPrice`).
        - `repository`: Spring Data JPA repositories for database operations.
        - `service`: Contains business logic, including the `StockApiService` which communicates with the external API.
- `src/main/resources`: Application resources.
    - `application.yaml`: The main Spring Boot configuration file.
    - `application-local.yaml`, `application-dev.yaml`: Profile-specific configurations.
    - `db/migration`: Flyway database migration scripts.
- `build.gradle`: The Gradle build script defining dependencies, plugins, and build settings.
- `Dockerfile`: For containerizing the application.
- `README.md`: Contains instructions for running the application.

## Architecture and Workflow

1. **Configuration:** On startup, the application's profile determines its configuration strategy.
    - **`local` profile:** It loads API keys and other secrets from a `.env` file at the project root.
    - **Other profiles (e.g., `dev`):** It connects to a Spring Cloud Config server to fetch its configuration, including database credentials and API keys.

2. **Database Setup:** The application uses Flyway to manage its database schema. For the `local` profile, it's configured to connect to an H2 database, which the `README.md` suggests running in a Docker container.

3. **API-Triggered Batch Jobs:** The `BatchController` exposes several REST endpoints (e.g., `/batch/price`, `/batch/corp-info`). When an HTTP POST request is sent to one of these endpoints, the corresponding Spring Batch job is triggered.

4. **Batch Processing:**
    - Each batch job (e.g., `CorpInfoBatch`) is defined as a Spring Bean and consists of one or more `Step`s.
    - A typical step is composed of a `Reader`, `Processor`, and `Writer`.
        - **Reader (`ItemReader`):** A custom `ItemReader` (e.g., `CorpInfoItemReader`) calls the `StockApiService`.
        - **Service (`StockApiService`):** This service uses `RestClient` to make HTTP requests to the `data.go.kr` API, fetching the raw data. It handles the API's pagination.
        - **Processor (`ItemProcessor`):** Processes the data fetched by the reader. In many cases, this is a simple pass-through.
        - **Writer (`ItemWriter`):** A `RepositoryItemWriter` is used to save the processed items (JPA entities) in chunks to the database via a Spring Data repository (e.g., `CorpInfoRepository`).

5. **Service Discovery:** For non-local profiles, the application registers itself with a Eureka service registry, allowing it to be discovered by other services in a microservices architecture.

## How to Run

### Prerequisites
- Java 17
- Docker

### Building the Application
```bash
./gradlew build
```

### Running with the `local` profile

1. **Create `.env` file:**
   ```bash
   cp .env.example .env
   ```
   Then, edit the `.env` file to add your actual API keys.

2. **Start H2 Database in Docker:**
   ```bash
   docker run -d --name h2-db-server \
     -p 1521:1521 \
     -v h2-data:/opt/h2-data \
     oscarfonts/h2
   ```

3. **Run the application:**
   ```bash
   java -jar -Dspring.profiles.active=local build/libs/stock-batch-0.0.1-SNAPSHOT.jar
   ```

### Triggering a Batch Job
- To fetch corporate information:
  ```bash
  curl -X POST "http://localhost:8085/batch/corp-info"
  ```
- To fetch stock prices for KOSPI:
  ```bash
  curl -X POST "http://localhost:8085/batch/price?market=KOSPI"
  ```
