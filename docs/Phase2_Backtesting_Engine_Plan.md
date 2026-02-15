# Phase 2: 백테스팅 엔진 구축 실행 계획 (Detailed Action Plan)

## 1. 목표
과거 데이터를 기반으로 퀀트 투자 전략의 유효성을 검증하는 **백테스팅 엔진**을 구축합니다. 동적 자산배분 및 정적 자산배분 전략을 시뮬레이션하고, 성과 지표(CAGR, MDD, Sharpe Ratio)를 자동 산출하여 전략의 실효성을 평가합니다.

## 2. 세부 실행 과제 (Action Items)

### 2.1. 신규 마이크로서비스 생성 [Critical]
- **[Task] `stock-strategy` 서비스 생성**
    - **목적**: 백테스팅 로직과 전략 관리를 독립적인 도메인 서비스로 분리
    - **포트**: 8084 (stock-corp: 8081, stock-finance: 8082, stock-price: 8083 다음)
    - **데이터베이스**: `stock_strategy` (MySQL 8.0, 포트 3310)
    - **주요 책임**:
        - 백테스팅 시뮬레이션 실행
        - 유니버스(Universe) 필터링 및 종목 선정
        - 전략 설정 및 성과 지표 계산
        - 백테스팅 결과 저장 및 조회

### 2.2. 유니버스(Universe) 선정 필터 구현 [High Priority]
- **[Task] 유니버스 필터링 엔진 개발**
    - **UniverseFilter 인터페이스 정의**:
        - `List<String> filter(LocalDate baseDate, UniverseFilterCriteria criteria)`: 기준일 기준 종목 코드 리스트 반환
    - **UniverseFilterCriteria 클래스**:
        - `market` (StockMarket): KOSPI, KOSDAQ, KONEX 등
        - `minMarketCap` (Long): 최소 시가총액
        - `maxMarketCap` (Long): 최대 시가총액
        - `excludeSectors` (List<String>): 제외할 업종 코드
        - `minTradingVolume` (Long): 최소 거래량
        - `customConditions` (Map<String, Object>): 추가 커스텀 조건
    - **구현 예시**:
        - "KOSPI 200 종목 중 시가총액 하위 20%"
        - "KOSDAQ 전체 중 거래량 상위 100개 종목"
        - "금융업 제외, 시가총액 1000억 이상"

- **[Task] 종목 데이터 조회 API 연동**
    - `stock-corp` 서비스의 내부 API를 통해 종목 정보 조회
    - `stock-finance` 서비스를 통해 재무 지표 기반 필터링
    - `stock-price` 서비스를 통해 가격 데이터 조회
    - **통신 방식**: HTTP 기반 REST API (Spring RestClient 또는 WebClient 사용)
    - 서비스 디스커버리(Eureka)를 통한 동적 엔드포인트 해석

### 2.3. 시뮬레이션 로직 구현 [Critical]
- **[Task] 백테스팅 시뮬레이션 엔진 개발**
    - **BacktestSimulation 엔티티 설계**:
        - `id` (Long, PK): 시뮬레이션 ID
        - `strategyName` (String): 전략 이름
        - `startDate` (LocalDate): 백테스팅 시작일
        - `endDate` (LocalDate): 백테스팅 종료일
        - `initialCapital` (BigDecimal): 초기 자본금
        - `rebalancingPeriod` (RebalancingPeriod): 리밸런싱 주기 (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)
        - `tradingFeeRate` (BigDecimal): 매매 수수료율 (예: 0.00015 = 0.015%)
        - `taxRate` (BigDecimal): 세금율 (예: 0.0023 = 0.23%)
        - `status` (SimulationStatus): 시뮬레이션 상태 (PENDING, RUNNING, COMPLETED, FAILED)
        - `createdAt` (LocalDateTime): 생성 시각
        - `completedAt` (LocalDateTime): 완료 시각

    - **SimulationStatus Enum**:
        - `PENDING`: 대기 중
        - `RUNNING`: 실행 중
        - `COMPLETED`: 완료
        - `FAILED`: 실패

    - **RebalancingPeriod Enum**:
        - `DAILY`: 일별
        - `WEEKLY`: 주별
        - `MONTHLY`: 월별
        - `QUARTERLY`: 분기별
        - `YEARLY`: 연별

- **[Task] 포트폴리오 상태 추적 로직**
    - **PortfolioSnapshot 엔티티**:
        - `id` (Long, PK)
        - `simulationId` (Long, FK): 시뮬레이션 ID
        - `snapshotDate` (LocalDate): 스냅샷 날짜
        - `totalValue` (BigDecimal): 총 자산 가치
        - `cashBalance` (BigDecimal): 현금 잔고
        - `holdings` (String/JSON): 보유 종목 및 수량 (JSON 형태)
    
    - **PortfolioHolding 클래스** (JSON 직렬화용):
        - `stockCode` (String): 종목 코드
        - `quantity` (Integer): 보유 수량
        - `averagePrice` (BigDecimal): 평균 매입가
        - `currentPrice` (BigDecimal): 현재가
        - `marketValue` (BigDecimal): 평가금액

- **[Task] 매매 시그널 생성 및 실행**
    - **TradingSignal 인터페이스**:
        - `List<TradeOrder> generateOrders(LocalDate rebalancingDate, Portfolio currentPortfolio, UniverseFilterCriteria criteria)`
    
    - **TradeOrder 클래스**:
        - `stockCode` (String): 종목 코드
        - `orderType` (OrderType): 매수/매도
        - `quantity` (Integer): 수량
        - `price` (BigDecimal): 주문 가격
        - `orderDate` (LocalDate): 주문 날짜
    
    - **OrderType Enum**:
        - `BUY`: 매수
        - `SELL`: 매도

    - **시뮬레이션 실행 흐름**:
        1. 시작일부터 종료일까지 일별로 순회
        2. 리밸런싱 주기 도래 시 유니버스 필터링 실행
        3. 전략에 따라 매매 시그널 생성
        4. 매매 수수료 및 세금 차감
        5. 포트폴리오 상태 업데이트 및 스냅샷 저장
        6. 다음 날짜로 이동

### 2.4. 성과 분석 지표 산출 [High Priority]
- **[Task] 성과 지표 계산 모듈 개발**
    - **BacktestResult 엔티티**:
        - `id` (Long, PK)
        - `simulationId` (Long, FK): 시뮬레이션 ID
        - `finalValue` (BigDecimal): 최종 자산 가치
        - `totalReturn` (BigDecimal): 총 수익률 (%)
        - `cagr` (BigDecimal): 연평균 성장률 (%)
        - `mdd` (BigDecimal): 최대 낙폭 (%)
        - `sharpeRatio` (BigDecimal): 샤프 지수
        - `volatility` (BigDecimal): 변동성 (%)
        - `winRate` (BigDecimal): 승률 (%)
        - `totalTrades` (Integer): 총 거래 횟수
        - `profitableTrades` (Integer): 수익 거래 횟수

    - **지표 계산 공식**:
        - **CAGR**: `((최종가치 / 초기자본) ^ (1 / 연수)) - 1`
        - **MDD**: `Max((고점 - 저점) / 고점)` (기간 중 최대값)
        - **Sharpe Ratio**: `(평균 수익률 - 무위험 수익률) / 수익률 표준편차`
        - **Volatility**: `일별 수익률의 표준편차 * sqrt(252)` (연환산)
        - **Win Rate**: `수익 거래 횟수 / 총 거래 횟수`

- **[Task] 성과 지표 계산 서비스**
    - `PerformanceCalculationService` 클래스 구현
    - 입력: `List<PortfolioSnapshot>` (시뮬레이션의 모든 스냅샷)
    - 출력: `BacktestResult` 엔티티

### 2.5. 전략 구현 프레임워크 [Medium Priority]
- **[Task] 전략 인터페이스 정의**
    - **Strategy 인터페이스**:
        - `String getName()`: 전략 이름
        - `List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe)`: 리밸런싱 로직
    
    - **기본 전략 구현 예시**:
        - **EqualWeightStrategy**: 유니버스 내 모든 종목 동일 비중
        - **MomentumStrategy**: 과거 N일 수익률 상위 종목 선정
        - **LowVolatilityStrategy**: 변동성 하위 종목 선정
        - **ValueStrategy**: PBR, PER 등 가치 지표 기반 선정

## 3. 데이터베이스 설계 (Database Schema)

### 3.1. Flyway Migration
- **파일명**: `services/stock-strategy/src/main/resources/db/migration/V1__init_strategy.sql`
- **테이블 목록**:
    - `TB_BACKTEST_SIMULATION`: 시뮬레이션 메타데이터
    - `TB_PORTFOLIO_SNAPSHOT`: 포트폴리오 스냅샷
    - `TB_BACKTEST_RESULT`: 성과 지표
    - `TB_TRADE_HISTORY`: 매매 이력 (선택적)

### 3.2. 테이블 스키마 예시

```sql
-- 백테스팅 시뮬레이션
CREATE TABLE TB_BACKTEST_SIMULATION (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_capital DECIMAL(19, 2) NOT NULL,
    rebalancing_period VARCHAR(20) NOT NULL,
    trading_fee_rate DECIMAL(10, 6) NOT NULL,
    tax_rate DECIMAL(10, 6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 포트폴리오 스냅샷
CREATE TABLE TB_PORTFOLIO_SNAPSHOT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_value DECIMAL(19, 2) NOT NULL,
    cash_balance DECIMAL(19, 2) NOT NULL,
    holdings JSON,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id),
    INDEX idx_simulation_date (simulation_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 백테스팅 결과
CREATE TABLE TB_BACKTEST_RESULT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL UNIQUE,
    final_value DECIMAL(19, 2) NOT NULL,
    total_return DECIMAL(10, 4),
    cagr DECIMAL(10, 4),
    mdd DECIMAL(10, 4),
    sharpe_ratio DECIMAL(10, 4),
    volatility DECIMAL(10, 4),
    win_rate DECIMAL(10, 4),
    total_trades INT,
    profitable_trades INT,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 매매 이력 (선택적)
CREATE TABLE TB_TRADE_HISTORY (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(10) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    fee DECIMAL(19, 2) NOT NULL,
    tax DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id),
    INDEX idx_simulation_date (simulation_id, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 4. API 설계 (REST Endpoints)

### 4.1. 백테스팅 실행 API
```
POST /api/v1/strategy/backtest
Request Body:
{
  "strategyName": "EqualWeight",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000000,
  "rebalancingPeriod": "MONTHLY",
  "tradingFeeRate": 0.00015,
  "taxRate": 0.0023,
  "universeFilter": {
    "market": "KOSPI",
    "minMarketCap": 100000000000,
    "excludeSectors": ["금융업"]
  }
}

Response:
{
  "simulationId": 1,
  "status": "PENDING",
  "message": "백테스팅이 시작되었습니다."
}
```

### 4.2. 백테스팅 결과 조회 API
```
GET /api/v1/strategy/backtest/{simulationId}/result

Response:
{
  "simulationId": 1,
  "strategyName": "EqualWeight",
  "period": {
    "startDate": "2020-01-01",
    "endDate": "2023-12-31"
  },
  "performance": {
    "initialCapital": 10000000,
    "finalValue": 15000000,
    "totalReturn": 50.0,
    "cagr": 14.47,
    "mdd": -25.3,
    "sharpeRatio": 1.25,
    "volatility": 18.5,
    "winRate": 62.5,
    "totalTrades": 48,
    "profitableTrades": 30
  }
}
```

### 4.3. 포트폴리오 스냅샷 조회 API
```
GET /api/v1/strategy/backtest/{simulationId}/snapshots?startDate=2020-01-01&endDate=2020-12-31

Response:
{
  "simulationId": 1,
  "snapshots": [
    {
      "date": "2020-01-01",
      "totalValue": 10000000,
      "cashBalance": 500000,
      "holdings": [
        {
          "stockCode": "005930",
          "quantity": 100,
          "averagePrice": "50000.00",
          "currentPrice": "52000.00",
          "marketValue": "5200000.00"
        }
      ]
    }
  ]
}
```

## 5. 구현 시 상세 가이드 (Implementation Details)

### 5.1. 의존성 추가 (Dependencies)
`services/stock-strategy/build.gradle`:
```gradle
def profile = project.findProperty('profile') ?: 'local'

println "Current profile: ${profile}"

configurations {
    all {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-undertow'
    
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    
    implementation 'org.tinylog:tinylog-api:2.6.2'
    implementation 'org.tinylog:tinylog-impl:2.6.2'
    
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'
    runtimeOnly 'com.mysql:mysql-connector-j'
    
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4'
    
    testImplementation 'org.springframework.batch:spring-batch-test'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    
    implementation 'io.github.cdimascio:dotenv-java:2.2.0'
    implementation project(':modules:stock-common')
}
```

**주요 특징**:
- Tomcat 제외, Undertow 사용 (기존 서비스와 동일)
- Tinylog 로깅 프레임워크 사용
- SpringDoc OpenAPI 문서화 지원
- dotenv-java로 환경 변수 관리
- stock-common 모듈 의존성 (RestClient 설정 재사용)

### 5.2. 패키지 구조 (Package Structure)
```
services/stock-strategy/
├── src/main/java/com/stock/strategy/
│   ├── StrategyApplication.java
│   ├── client/
│   │   ├── CorpClient.java          # stock-corp HTTP 연동
│   │   ├── FinanceClient.java       # stock-finance HTTP 연동
│   │   └── PriceClient.java         # stock-price HTTP 연동
│   ├── config/
│   │   ├── DbConfig.java
│   │   └── BatchConfig.java             # 배치 설정 (필요시)
│   ├── controller/
│   │   └── BacktestController.java
│   ├── entity/
│   │   ├── BacktestSimulation.java
│   │   ├── PortfolioSnapshot.java
│   │   ├── BacktestResult.java
│   │   └── TradeHistory.java
│   ├── repository/
│   │   ├── BacktestSimulationRepository.java
│   │   ├── PortfolioSnapshotRepository.java
│   │   ├── BacktestResultRepository.java
│   │   └── TradeHistoryRepository.java
│   ├── service/
│   │   ├── BacktestService.java              # 백테스팅 실행
│   │   ├── UniverseFilterService.java        # 유니버스 필터링
│   │   ├── SimulationEngine.java             # 시뮬레이션 엔진
│   │   ├── PerformanceCalculationService.java # 성과 지표 계산
│   │   └── StrategyFactory.java              # 전략 팩토리
│   ├── strategy/
│   │   ├── Strategy.java                     # 전략 인터페이스
│   │   ├── EqualWeightStrategy.java
│   │   ├── MomentumStrategy.java
│   │   └── LowVolatilityStrategy.java
│   └── dto/
│       ├── BacktestRequest.java
│       ├── BacktestResponse.java
│       ├── UniverseFilterCriteria.java
│       ├── PortfolioHolding.java
│       └── TradeOrder.java
```

### 5.3. 시뮬레이션 실행 흐름 (Simulation Flow)

**1) 백테스팅 요청 접수**
- `BacktestController`에서 요청 수신
- `BacktestSimulation` 엔티티 생성 및 저장 (status: PENDING)
- 비동기 처리를 위해 `@Async` 또는 별도 스레드로 시뮬레이션 실행

**2) 시뮬레이션 초기화**
- 초기 포트폴리오 생성 (전액 현금)
- 시작일 설정

**3) 일별 시뮬레이션 루프**
```java
for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
    // 1. 휴장일 체크 (DayOffService 활용)
    if (isDayOff(date)) continue;
    
    // 2. 리밸런싱 주기 확인
    if (isRebalancingDate(date, rebalancingPeriod)) {
        // 3. 유니버스 필터링
        List<String> universe = universeFilterService.filter(date, criteria);
        
        // 4. 전략에 따른 매매 시그널 생성
        List<TradeOrder> orders = strategy.rebalance(date, currentPortfolio, universe);
        
        // 5. 매매 실행 (수수료 및 세금 차감)
        executeOrders(orders, currentPortfolio);
    }
    
    // 6. 포트폴리오 평가 (현재가 기준)
    updatePortfolioValue(date, currentPortfolio);
    
    // 7. 스냅샷 저장 (선택적: 리밸런싱일 또는 월말만)
    if (shouldSaveSnapshot(date)) {
        saveSnapshot(simulationId, date, currentPortfolio);
    }
}
```

**4) 성과 지표 계산**
- 모든 스냅샷 조회
- CAGR, MDD, Sharpe Ratio 등 계산
- `BacktestResult` 저장

**5) 시뮬레이션 완료**
- `BacktestSimulation` 상태 업데이트 (status: COMPLETED)
- `completedAt` 시각 기록

### 5.4. 기술적 고려사항 (Technical Considerations)

**1) 성능 최적화**
- 대량의 과거 데이터 조회 시 **배치 조회** 활용
- 일별 가격 데이터는 **캐싱** 고려 (Redis 또는 In-Memory Map)
- 장기 백테스팅(10년 이상) 시 **병렬 처리** 검토

**2) 데이터 정합성**
- 수정주가(Adjusted Price) 사용 필수
- 휴장일 처리 로직 필수 (`DayOffService` 활용)
- 상장폐지 종목 처리 (유니버스에서 자동 제외)

**3) 비동기 처리**
- 백테스팅은 시간이 오래 걸리므로 **비동기 실행** 권장
- Spring `@Async` 또는 별도 배치 Job으로 구현
- 진행 상황 추적을 위한 상태 업데이트

**4) 에러 처리**
- 시뮬레이션 중 오류 발생 시 `status: FAILED` 처리
- 에러 메시지 로깅 및 사용자 알림

### 5.5. Application Configuration (application.yaml)

`services/stock-strategy/src/main/resources/application.yaml`:
```yaml
server:
  port: 8084

spring:
  application:
    name: stock-strategy
  config:
    import: "optional:file:.env[.properties]"
  
  # DB Migration
  flyway:
    enabled: true
    locations: classpath:db/migration
  
  # JPA
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

  # Datasource
  datasource:
    url: ${STOCK_STRATEGY_DB_URL:jdbc:mysql://localhost:3310/stock_strategy?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
    username: ${STRATEGY_DB_USER:strategy_user}
    password: ${STRATEGY_DB_PASSWORD:strategy_pass}
    batch:
      url: ${STOCK_BATCH_DB_URL:jdbc:mysql://localhost:3309/stock_batch?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
      username: ${BATCH_DB_USER:batch_user}
      password: ${BATCH_DB_PASSWORD:batch_pass}

  # Batch
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always

# Inter-service communication URLs
services:
  stock-corp:
    url: ${STOCK_CORP_URL:http://localhost:8081}
  stock-price:
    url: ${STOCK_PRICE_URL:http://localhost:8083}
  stock-finance:
    url: ${STOCK_FINANCE_URL:http://localhost:8082}

---
spring:
  config:
    activate:
      on-profile: local
  cloud:
    config:
      enabled: false

eureka:
  client:
    enabled: false

---
spring:
  config:
    activate:
      on-profile: prod

eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://${EUREKA_USER:admin}:${EUREKA_PASSWORD:1234}@stock-discovery:8761/eureka

# Docker 환경에서는 서비스 이름으로 통신
services:
  stock-corp:
    url: http://stock-corp:8081
  stock-price:
    url: http://stock-price:8083
  stock-finance:
    url: http://stock-finance:8082
```

**주요 설정**:
- **포트**: 8084 (기존 서비스들과 순차적)
- **멀티 데이터소스**: 메인 DB(stock_strategy) + 배치 메타데이터 DB(stock_batch)
- **Flyway**: 자동 마이그레이션 활성화
- **Batch Job**: 자동 실행 비활성화 (API 트리거 방식)
- **프로파일별 URL**: local은 localhost, prod는 Docker 서비스 이름 사용
- **dotenv 통합**: `.env` 파일에서 환경 변수 자동 로드

### 5.6. HTTP Client 구현 가이드

**현재 stock-finance 서비스의 구현 방식을 따릅니다** (RestClient 사용).

**RestClientConfig 설정** (modules/stock-common에 이미 존재):
```java
@Configuration
public class RestClientConfig {

    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        return mapper;
    }

    @Bean
    public RestClient restClient(ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }
}
```

**CorpClient 예시** (stock-finance 패턴 참고):
```java
@Component
@RequiredArgsConstructor
public class CorpClient {
    
    private final RestClient restClient;
    
    @Value("${services.stock-corp.url:http://localhost:8081}")
    private String corpServiceUrl;
    
    public List<CorpInfoDto> getCorpsByMarket(String market, String date) {
        return restClient.get()
            .uri(corpServiceUrl + "/api/v1/corp/internal/corps?market=" + market + "&date=" + date)
            .retrieve()
            .body(new ParameterizedTypeReference<List<CorpInfoDto>>() {});
    }
    
    public CorpInfoDto getCorpInfo(String corpCode) {
        return restClient.get()
            .uri(corpServiceUrl + "/api/v1/corp/internal/" + corpCode)
            .retrieve()
            .body(CorpInfoDto.class);
    }
}
```

**PriceClient 예시** (stock-finance 패턴 참고):
```java
@Component
@RequiredArgsConstructor
public class PriceClient {
    
    private final RestClient restClient;
    
    @Value("${services.stock-price.url:http://localhost:8083}")
    private String priceServiceUrl;
    
    public List<StockPriceDto> getPriceHistory(String stockCode, String startDate, String endDate) {
        return restClient.get()
            .uri(priceServiceUrl + "/api/v1/stock/internal/prices/" + stockCode 
                + "?startDate=" + startDate + "&endDate=" + endDate)
            .retrieve()
            .body(new ParameterizedTypeReference<List<StockPriceDto>>() {});
    }
    
    public StockPriceDto getPriceByDate(String stockCode, String date) {
        return restClient.get()
            .uri(priceServiceUrl + "/api/v1/stock/internal/price/" + stockCode + "/" + date)
            .retrieve()
            .body(StockPriceDto.class);
    }
}
```

**FinanceClient 예시** (stock-finance 패턴 참고):
```java
@Component
@RequiredArgsConstructor
public class FinanceClient {
    
    private final RestClient restClient;
    
    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;
    
    public List<CorpFinanceDto> getFinanceByStockCode(String stockCode, String startDate, String endDate) {
        return restClient.get()
            .uri(financeServiceUrl + "/api/v1/finance/internal/" + stockCode 
                + "?startDate=" + startDate + "&endDate=" + endDate)
            .retrieve()
            .body(new ParameterizedTypeReference<List<CorpFinanceDto>>() {});
    }
}
```

**주요 특징**:
- **간단한 설정**: stock-common의 RestClientConfig Bean을 주입받아 사용
- **프로파일별 URL**: application.yaml에서 local/prod 환경별 URL 설정
- **타임아웃 설정**: JdkClientHttpRequestFactory로 연결(5초)/읽기(10초) 타임아웃 설정
- **ObjectMapper 커스터마이징**: 빈 문자열을 null로 처리, primitive 타입 null 체크
- **에러 핸들링**: 필요시 `onStatus()` 메서드로 HTTP 상태 코드별 처리 가능

## 6. 테스트 전략 (Testing Strategy)

### 6.1. 단위 테스트
- 성과 지표 계산 로직 테스트
- 유니버스 필터링 로직 테스트
- 전략별 리밸런싱 로직 테스트

### 6.2. 통합 테스트
- 전체 시뮬레이션 실행 테스트 (짧은 기간)
- HTTP Client 연동 테스트 (Mock 서버 또는 실제 서비스)
- 서비스 간 통신 타임아웃 및 에러 핸들링 테스트

### 6.3. 성능 테스트
- 10년 백테스팅 실행 시간 측정
- 대량 종목(100개 이상) 시뮬레이션 테스트

## 7. 추가 설정 파일 (Additional Configuration Files)

### 7.1. Dockerfile
`services/stock-strategy/Dockerfile`:
```dockerfile
FROM amazoncorretto:21-alpine

WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 7.2. Tinylog Configuration
`services/stock-strategy/src/main/resources/tinylog.properties`:
```properties
# logs to Console
writer        = console
writer.format = {date: HH:mm:ss.SSS} {level}: {message}
writer.level  = debug


writerFile          = rolling file
writerFile.level    = info
writerFile.format   = {date: HH:mm:ss.SSS} {level}: {class}.{method}() {message}

writerFile.file     = /logs/{date:yyyy-MM-dd}-log.{count}.log
writerFile.latest   = /logs/latest.log
writerFile.charset  = UTF-8
writerFile.buffered = true
writerFile.policies = startup, daily: 00:00, size: 10mb
writerFile.backups  = 30
writerFile.convert  = gzip
```

## 8. Docker Compose 설정 업데이트

`docker-compose.yaml`에 `stock-strategy` 서비스 추가:

```yaml
stock-strategy:
  build: ./services/stock-strategy
  container_name: stock-strategy
  ports:
    - "8084:8084"
  env_file: .env
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://${EUREKA_USER}:${EUREKA_PASSWORD}@stock-discovery:8761/eureka
    - SPRING_DATASOURCE_URL=${STOCK_STRATEGY_DB_URL}
    - SPRING_DATASOURCE_USERNAME=${STRATEGY_DB_USER}
    - SPRING_DATASOURCE_PASSWORD=${STRATEGY_DB_PASSWORD}
    - SPRING_DATASOURCE_BATCH_URL=${STOCK_BATCH_DB_URL}
    - SPRING_DATASOURCE_BATCH_USERNAME=${BATCH_DB_USER}
    - SPRING_DATASOURCE_BATCH_PASSWORD=${BATCH_DB_PASSWORD}
  networks:
    - stock-network
  depends_on:
    stock-discovery:
      condition: service_healthy
    stock-strategy-db:
      condition: service_healthy
    stock-batch-db:
      condition: service_healthy

stock-strategy-db:
  image: mysql/mysql-server:8.0
  container_name: stock-strategy-db
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    MYSQL_DATABASE: stock_strategy
    MYSQL_USER: ${STRATEGY_DB_USER}
    MYSQL_PASSWORD: ${STRATEGY_DB_PASSWORD}
    MYSQL_ROOT_HOST: "%"
  ports:
    - "3310:3306"
  volumes:
    - strategy-db-volume:/var/lib/mysql
  command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
  networks:
    - stock-network
  healthcheck:
    test: ["CMD", "mysqladmin" ,"ping", "-h", "localhost"]
    interval: 10s
    timeout: 5s
    retries: 5

volumes:
  strategy-db-volume:
```

`.env` 파일에 추가:
```properties
# Strategy Service DB
STOCK_STRATEGY_DB_URL=jdbc:mysql://stock-strategy-db:3306/stock_strategy?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
STRATEGY_DB_USER=strategy_user
STRATEGY_DB_PASSWORD=strategy_pass
```

## 8. Gateway 라우팅 설정

`stock-gateway`의 `application.yaml`에 라우팅 규칙 추가:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: stock-strategy
          uri: lb://stock-strategy
          predicates:
            - Path=/api/v1/strategy/**
          filters:
            - RewritePath=/api/v1/strategy/(?<segment>.*), /${segment}
```

## 9. 결론 및 다음 단계

Phase 2는 **백테스팅 엔진의 핵심 기능 구현**에 집중합니다. 

**핵심 구현 순서**:
1. `stock-strategy` 서비스 생성 및 기본 인프라 구축
2. 유니버스 필터링 로직 구현
3. 시뮬레이션 엔진 개발 (포트폴리오 추적, 매매 실행)
4. 성과 지표 계산 모듈 구현
5. 기본 전략 구현 (EqualWeight, Momentum)
6. API 개발 및 테스트

**Phase 3 준비사항**:
- Phase 2 완료 후, 실제 포트폴리오 관리 및 리밸런싱 배치로 확장
- 백테스팅 결과를 기반으로 실전 투자 전략 수립


---

# Phase 2: 백테스팅 엔진 구축 - 추가 개발 계획 (2026-02-07 업데이트)

## 📋 현재 상태 요약 (2026-02-07)

Phase 2의 핵심 백테스팅 엔진은 **대부분 완료**되었으나, 로드맵에 명시된 일부 고급 기능들이 아직 구현되지 않았습니다.

### ✅ 완료된 핵심 기능
- stock-strategy 서비스 생성 및 인프라 구축
- 시뮬레이션 엔진 (일별 루프, 리밸런싱, 매매 실행)
- 성과 지표 계산 (CAGR, MDD, Sharpe Ratio, Volatility, Win Rate)
- 유니버스 필터링 (시장, 시가총액, 거래량, 업종)
- EqualWeightStrategy 구현
- REST API 및 서비스 간 통신
- 데이터베이스 스키마 및 마이그레이션

### 🚧 로드맵 대비 누락된 기능

로드맵의 **Phase 2 완료 기준**과 비교했을 때, 다음 기능들이 아직 구현되지 않았습니다:

---

## 🎯 추가 개발 필요 항목

### 1. 추가 전략 구현 [High Priority]

로드맵에서 언급된 전략들이 아직 구현되지 않았습니다.

#### 1.1. MomentumStrategy (모멘텀 전략)
**목적**: 과거 수익률이 높은 종목이 단기적으로 지속 상승하는 경향을 활용

**구현 계획**:
```java
@Component
public class MomentumStrategy implements Strategy {
    
    @Override
    public String getName() {
        return "Momentum";
    }
    
    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        // 1. 유니버스 내 모든 종목의 과거 N일 수익률 계산
        //    - 1개월(20일), 3개월(60일), 6개월(120일) 모멘텀 지표 활용
        //    - PriceClient를 통해 과거 가격 데이터 조회
        
        // 2. 모멘텀 스코어 계산
        //    - 예: (1개월 수익률 * 0.5) + (3개월 수익률 * 0.3) + (6개월 수익률 * 0.2)
        
        // 3. 상위 N개 종목 선정 (예: 상위 20개)
        
        // 4. 동일 비중 배분하여 매수/매도 주문 생성
        
        return orders;
    }
}
```

**필요 데이터**:
- `TB_STOCK_INDICATOR` 테이블의 `momentum1m`, `momentum3m`, `momentum6m` 컬럼 활용
- PriceClient를 통한 과거 가격 데이터 조회

**예상 작업 시간**: 2일

---

#### 1.2. LowVolatilityStrategy (저변동성 전략)
**목적**: 변동성이 낮은 종목이 장기적으로 안정적인 수익을 제공하는 경향 활용

**구현 계획**:
```java
@Component
public class LowVolatilityStrategy implements Strategy {
    
    @Override
    public String getName() {
        return "LowVolatility";
    }
    
    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        // 1. 유니버스 내 모든 종목의 변동성 계산
        //    - 최근 60일 또는 120일 일별 수익률의 표준편차
        //    - PriceClient를 통해 과거 가격 데이터 조회
        
        // 2. 변동성 오름차순 정렬
        
        // 3. 하위 N개 종목 선정 (예: 하위 20개)
        
        // 4. 동일 비중 배분하여 매수/매도 주문 생성
        
        return orders;
    }
}
```

**필요 데이터**:
- PriceClient를 통한 과거 가격 데이터 조회
- 일별 수익률 계산 및 표준편차 산출

**예상 작업 시간**: 2일

---

#### 1.3. ValueStrategy (가치 투자 전략)
**목적**: 저평가된 종목(낮은 PER, PBR, 높은 ROE)을 선정하여 장기 투자

**구현 계획**:
```java
@Component
public class ValueStrategy implements Strategy {
    
    private final FinanceClient financeClient;
    
    @Override
    public String getName() {
        return "Value";
    }
    
    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        // 1. 유니버스 내 모든 종목의 재무 지표 조회
        //    - FinanceClient를 통해 최신 재무 데이터 조회
        //    - PER, PBR, ROE, ROA 등
        
        // 2. 가치 스코어 계산
        //    - 예: (1 / PER) + (1 / PBR) + (ROE / 100)
        //    - 낮은 PER, PBR일수록 높은 점수
        //    - 높은 ROE일수록 높은 점수
        
        // 3. 상위 N개 종목 선정 (예: 상위 20개)
        
        // 4. 동일 비중 배분하여 매수/매도 주문 생성
        
        return orders;
    }
}
```

**필요 데이터**:
- `TB_CORP_FINANCE` 테이블의 `per`, `pbr`, `roe`, `roa` 컬럼 활용
- FinanceClient를 통한 재무 데이터 조회

**예상 작업 시간**: 2일

---

#### 1.4. StrategyFactory 업데이트
**목적**: 새로운 전략들을 팩토리에 등록

```java
@Component
@RequiredArgsConstructor
public class StrategyFactory {
    
    private final EqualWeightStrategy equalWeightStrategy;
    private final MomentumStrategy momentumStrategy;
    private final LowVolatilityStrategy lowVolatilityStrategy;
    private final ValueStrategy valueStrategy;
    
    public Strategy getStrategy(String strategyName) {
        return switch (strategyName.toUpperCase()) {
            case "EQUALWEIGHT" -> equalWeightStrategy;
            case "MOMENTUM" -> momentumStrategy;
            case "LOWVOLATILITY" -> lowVolatilityStrategy;
            case "VALUE" -> valueStrategy;
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        };
    }
    
    public List<String> getAvailableStrategies() {
        return List.of("EqualWeight", "Momentum", "LowVolatility", "Value");
    }
}
```

**예상 작업 시간**: 0.5일

---

### 2. 성능 최적화 [High Priority]

현재 구현은 기능적으로 완성되었으나, 장기 백테스팅(10년 이상) 시 성능 문제가 발생할 수 있습니다.

#### 2.1. 가격 데이터 캐싱
**문제**: 동일한 종목의 가격 데이터를 반복 조회하여 성능 저하

**해결 방안**:
```java
@Service
@RequiredArgsConstructor
public class PriceDataCache {
    
    private final PriceClient priceClient;
    private final Map<String, List<StockPriceDto>> cache = new ConcurrentHashMap<>();
    
    public List<StockPriceDto> getPriceHistory(String stockCode, LocalDate startDate, LocalDate endDate) {
        String cacheKey = stockCode + "_" + startDate + "_" + endDate;
        
        return cache.computeIfAbsent(cacheKey, k -> 
            priceClient.getPriceHistory(stockCode, startDate.toString(), endDate.toString())
        );
    }
    
    public void clearCache() {
        cache.clear();
    }
}
```

**대안**: Redis 캐시 도입 (Phase 3 이후 검토)

**예상 작업 시간**: 1일

---

#### 2.2. 배치 조회 최적화
**문제**: 유니버스 필터링 시 종목별로 개별 조회하여 N+1 문제 발생

**해결 방안**:
- `CorpClient`, `PriceClient`, `FinanceClient`에 **배치 조회 API** 추가
- 예: `List<CorpInfoDto> getCorpsByStockCodes(List<String> stockCodes)`

**stock-corp 서비스에 추가 필요**:
```java
@GetMapping("/internal/corps/batch")
public List<CorpInfoDto> getCorpsByStockCodes(@RequestParam List<String> stockCodes) {
    return corpService.getCorpsByStockCodes(stockCodes);
}
```

**예상 작업 시간**: 2일 (각 서비스에 배치 API 추가)

---

#### 2.3. 병렬 처리
**문제**: 장기 백테스팅(10년 이상) 시 실행 시간이 길어짐

**해결 방안**:
```java
@Service
public class SimulationEngine {
    
    @Async
    public CompletableFuture<BacktestResult> runSimulationAsync(BacktestSimulation simulation) {
        // 기존 시뮬레이션 로직
        BacktestResult result = runSimulation(simulation);
        return CompletableFuture.completedFuture(result);
    }
}
```

**추가 설정**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("backtest-");
        executor.initialize();
        return executor;
    }
}
```

**예상 작업 시간**: 1일

---

### 3. 비동기 처리 개선 [Medium Priority]

현재 백테스팅은 동기 방식으로 실행되어 요청 후 완료까지 대기해야 합니다.

#### 3.1. 비동기 백테스팅 실행
**목표**: 백테스팅 요청 즉시 응답, 백그라운드 실행

**구현 계획**:
```java
@PostMapping("/backtest")
public ResponseEntity<BacktestResponse> startBacktest(@RequestBody BacktestRequest request) {
    // 1. BacktestSimulation 엔티티 생성 및 저장 (status: PENDING)
    BacktestSimulation simulation = backtestService.createSimulation(request);
    
    // 2. 비동기 실행
    simulationEngine.runSimulationAsync(simulation);
    
    // 3. 즉시 응답
    return ResponseEntity.ok(new BacktestResponse(
        simulation.getId(),
        "PENDING",
        "백테스팅이 시작되었습니다. 결과는 /api/v1/strategy/backtest/" + simulation.getId() + "/result 에서 확인하세요."
    ));
}
```

**예상 작업 시간**: 0.5일

---

#### 3.2. 진행 상황 추적 API
**목표**: 실시간 진행률 조회

**구현 계획**:
```java
@GetMapping("/backtest/{id}/progress")
public ResponseEntity<ProgressResponse> getProgress(@PathVariable Long id) {
    BacktestSimulation simulation = simulationRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Simulation not found"));
    
    // 진행률 계산: (현재 처리된 날짜 수 / 전체 날짜 수) * 100
    int totalDays = (int) ChronoUnit.DAYS.between(simulation.getStartDate(), simulation.getEndDate());
    int processedDays = portfolioSnapshotRepository.countBySimulationId(id);
    double progress = (double) processedDays / totalDays * 100;
    
    return ResponseEntity.ok(new ProgressResponse(
        simulation.getId(),
        simulation.getStatus().name(),
        progress,
        processedDays,
        totalDays
    ));
}
```

**예상 작업 시간**: 1일

---

#### 3.3. WebSocket 실시간 알림 (선택적)
**목표**: 백테스팅 완료 시 실시간 알림

**구현 계획**:
- Spring WebSocket 설정
- 백테스팅 완료 시 WebSocket으로 알림 전송
- 프론트엔드에서 실시간 수신

**예상 작업 시간**: 2일 (Phase 5 시각화와 함께 진행 권장)

---

### 4. 유니버스 필터링 고도화 [Medium Priority]

현재 유니버스 필터링은 기본적인 조건만 지원합니다.

#### 4.1. 복합 조건 지원
**목표**: AND/OR 조건 조합 지원

**구현 계획**:
```java
public class UniverseFilterCriteria {
    private StockMarket market;
    private Long minMarketCap;
    private Long maxMarketCap;
    private List<String> excludeSectors;
    private Long minTradingVolume;
    
    // 추가: 복합 조건
    private List<FilterCondition> customConditions;
    
    public static class FilterCondition {
        private String field;        // 예: "per", "pbr", "roe"
        private String operator;     // 예: "GT", "LT", "EQ", "BETWEEN"
        private Object value;        // 예: 10, [5, 15]
        private String logicalOp;    // 예: "AND", "OR"
    }
}
```

**예상 작업 시간**: 2일

---

#### 4.2. 동적 쿼리 생성 (QueryDSL)
**목표**: 복잡한 필터링 조건을 동적으로 쿼리 생성

**구현 계획**:
- QueryDSL 의존성 추가
- `UniverseFilterService`에서 동적 쿼리 생성
- 성능 최적화 (인덱스 활용)

**예상 작업 시간**: 3일

---

### 5. 테스트 및 검증 [High Priority]

현재 테스트 코드가 부족합니다.

#### 5.1. 단위 테스트
**목표**: 핵심 로직 테스트 커버리지 80% 이상

**테스트 대상**:
- `PerformanceCalculationService`: 성과 지표 계산 로직
- `EqualWeightStrategy`, `MomentumStrategy` 등: 전략별 리밸런싱 로직
- `UniverseFilterService`: 유니버스 필터링 로직

**예상 작업 시간**: 3일

---

#### 5.2. 통합 테스트
**목표**: 전체 시뮬레이션 플로우 검증

**테스트 시나리오**:
- 짧은 기간(1개월) 백테스팅 실행
- 서비스 간 통신 검증
- 에러 핸들링 검증

**예상 작업 시간**: 2일

---

#### 5.3. 실제 데이터 검증
**목표**: 알려진 전략 재현 및 성과 지표 정확도 검증

**검증 방법**:
- 저PBR 전략 백테스팅 실행
- 외부 백테스팅 도구(예: QuantConnect) 결과와 비교
- CAGR, MDD, Sharpe Ratio 오차 5% 이내 확인

**예상 작업 시간**: 2일

---

### 6. 로드맵 추가 기능 (Phase 2 범위 외)

로드맵에는 있지만 Phase 2 범위를 벗어나는 기능들입니다. Phase 3 이후 진행 권장.

#### 6.1. 팩터 기반 전략 (Phase 4)
- 멀티 팩터 조합
- 팩터 스코어링 시스템
- 팩터 간 상관관계 분석

#### 6.2. 전략 최적화 엔진 (Phase 4)
- 파라미터 그리드 서치
- 유전 알고리즘 기반 최적화
- Walk-forward 분석

#### 6.3. 머신러닝 통합 (Phase 4)
- 종목 선정 모델
- 수익률 예측 모델
- Python 연동 (Py4J 또는 REST API)

---

## 📅 추가 개발 일정 (예상)

### Week 1-2: 전략 구현 및 최적화
- [ ] MomentumStrategy 구현 (2일)
- [ ] LowVolatilityStrategy 구현 (2일)
- [ ] ValueStrategy 구현 (2일)
- [ ] 가격 데이터 캐싱 (1일)
- [ ] 배치 조회 최적화 (2일)
- [ ] 병렬 처리 (1일)

**예상 완료**: 2026-02-21

### Week 3: 비동기 처리 및 진행 상황 추적
- [ ] 비동기 백테스팅 실행 (0.5일)
- [ ] 진행 상황 추적 API (1일)
- [ ] 유니버스 필터링 고도화 (2일)

**예상 완료**: 2026-02-28

### Week 4: 테스트 및 검증
- [ ] 단위 테스트 작성 (3일)
- [ ] 통합 테스트 작성 (2일)
- [ ] 실제 데이터 검증 (2일)

**예상 완료**: 2026-03-07

---

## 🎯 Phase 2 완료 기준 (Updated)

Phase 2는 다음 조건을 모두 만족할 때 완료로 간주합니다:

### 필수 (Must Have)
- ✅ stock-strategy 서비스 생성 및 인프라 구축
- ✅ 시뮬레이션 엔진 구현
- ✅ 성과 지표 계산 (CAGR, MDD, Sharpe Ratio, Volatility, Win Rate)
- ✅ EqualWeightStrategy 구현
- [ ] **MomentumStrategy 구현**
- [ ] **LowVolatilityStrategy 구현**
- [ ] **ValueStrategy 구현**
- [ ] **가격 데이터 캐싱**
- [ ] **비동기 백테스팅 실행**
- [ ] **단위 테스트 커버리지 80% 이상**
- [ ] **실제 데이터 검증 완료**

### 권장 (Should Have)
- [ ] 배치 조회 최적화
- [ ] 병렬 처리
- [ ] 진행 상황 추적 API
- [ ] 유니버스 필터링 고도화
- [ ] 통합 테스트

### 선택 (Nice to Have)
- [ ] WebSocket 실시간 알림
- [ ] 동적 쿼리 생성 (QueryDSL)

---

## 📝 결론

Phase 2의 **핵심 백테스팅 엔진은 완성**되었으나, 로드맵에서 요구하는 **추가 전략 구현, 성능 최적화, 테스트**가 아직 남아있습니다.

**우선순위**:
1. **추가 전략 구현** (Momentum, LowVolatility, Value) - 백테스팅 엔진의 실용성 확보
2. **성능 최적화** (캐싱, 배치 조회) - 장기 백테스팅 실행 가능
3. **테스트 및 검증** - 신뢰성 확보
4. **비동기 처리** - 사용자 경험 개선

**예상 완료 시점**: 2026-03-07 (약 4주 소요)

Phase 2 완료 후, Phase 3 (포트폴리오 관리 및 리밸런싱)로 진행할 수 있습니다.


---

# Phase 2: 로드맵 정합성 업데이트 (2026-02-09)

## 🎯 핵심 기술 스택 및 정밀도 표준
- **기술 분석 라이브러리**: `ta4j-core:0.15` 활용 (전략 지표 계산용)
- **데이터 정밀도**: 모든 금융 데이터(자본금, 평가액 등)는 `DECIMAL(25, 4)` 및 `BigDecimal`을 표준으로 사용합니다. (매매 수수료 및 세금율은 필요에 따라 `DECIMAL(10, 6)` 유지)

## 📈 성공 지표 (KPI)
백테스팅 엔진의 성능 및 안정성을 위해 다음 지표를 준수합니다:
- **실행 성능**:
    - 10년치 데이터 백테스팅 실행 시간: 5분 이내
    - 100개 종목 동시 시뮬레이션: 10분 이내
- **처리 능력**:
    - 동시 백테스팅 요청 처리: 10개 이상 가능
- **데이터 품질**:
    - 기술적 지표 계산 정확도: 100% (검증 완료 필수)

## 🚀 향후 고도화 항목 (Immediate Next Steps)
1. **전략 다변화**: Momentum, Value, LowVolatility 전략 우선 구현
2. **성능 최적화**: Redis 캐싱 도입 및 `CompletableFuture` 기반 병렬 처리 적용
3. **비동기 개선**: `@Async` 실행 및 WebSocket 기반 실시간 진행률 알림 구현
