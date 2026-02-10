# ValueStrategy 구현 실행 계획

**작성일**: 2026-02-10  
**목표**: Phase 2 백테스팅 엔진의 가치 투자 전략(ValueStrategy) 구현

---

## 📋 개요

ValueStrategy는 저평가된 종목(낮은 PER, PBR, 높은 ROE)을 선정하여 장기 투자하는 퀀트 전략입니다.

### 핵심 로직
- **PER, PBR**: 낮을수록 저평가 → 역수(`1/PER`, `1/PBR`) 계산
- **ROE**: 높을수록 수익성 우수 → 그대로 사용
- **가중치**: 유연하게 설정 가능 (기본값: PER 30%, PBR 30%, ROE 40%)
- **종목 선정**: 가치 스코어 상위 N개 종목 (기본값: 20개)

---

## 🎯 구현 단계

### Step 0: BacktestRequest에 전략 설정 필드 추가

**파일**: `services/stock-strategy/src/main/java/com/stock/strategy/dto/BacktestRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestRequest {
    private StrategyType strategyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private RebalancingPeriod rebalancingPeriod;
    private BigDecimal tradingFeeRate;
    private BigDecimal taxRate;
    private UniverseFilterCriteria universeFilter;
    
    // 추가: 전략별 설정
    private ValueStrategyConfig valueStrategyConfig;  // Value 전략 설정 (선택)
}
```

**SimulationEngine 수정**: `services/stock-strategy/src/main/java/com/stock/strategy/service/SimulationEngine.java`

```java
// rebalance 호출 시 전략 설정 전달
if (strategy instanceof ValueStrategy && simulation.getValueStrategyConfig() != null) {
    orders = ((ValueStrategy) strategy).rebalance(date, portfolio, universe, 
                                                  simulation.getValueStrategyConfig());
} else {
    orders = strategy.rebalance(date, portfolio, universe);
}
```

**BacktestSimulation 엔티티 수정**: JSON 컬럼 추가

```java
@Entity
@Table(name = "TB_BACKTEST_SIMULATION")
public class BacktestSimulation {
    // ... 기존 필드들
    
    @Column(name = "value_strategy_config", columnDefinition = "JSON")
    private String valueStrategyConfigJson;  // JSON 직렬화된 설정
    
    @Transient
    public ValueStrategyConfig getValueStrategyConfig() {
        if (valueStrategyConfigJson == null) return null;
        // JSON 역직렬화
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(valueStrategyConfigJson, ValueStrategyConfig.class);
    }
}
```

**예상 시간**: 0.5일

---

### Step 1: stock-finance 서비스에 내부 API 추가

**파일 1**: `services/stock-finance/src/main/java/com/stock/finance/mapper/CorpFinanceIndicatorMapper.java` (신규)

```java
@Mapper(componentModel = "spring")
public interface CorpFinanceIndicatorMapper {
    CorpFinanceIndicatorDto toDto(CorpFinanceIndicator entity);
    List<CorpFinanceIndicatorDto> toDtoList(List<CorpFinanceIndicator> entities);
}
```

**파일 2**: `services/stock-finance/src/main/java/com/stock/finance/controller/InternalFinanceController.java` (신규)

```java
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalFinanceController {

    private final CorpFinanceIndicatorRepository indicatorRepository;
    private final CorpFinanceIndicatorMapper mapper;

    @GetMapping("/indicators/batch")
    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(
            @RequestParam List<String> stockCodes,
            @RequestParam String date) {
        
        LocalDate basDt = LocalDate.parse(date);
        
        return mapper.toDtoList(
            indicatorRepository.findByCorpCodeInAndBasDt(stockCodes, basDt)
        );
    }

    @GetMapping("/indicators/{stockCode}/latest")
    public CorpFinanceIndicatorDto getLatestIndicator(@PathVariable String stockCode) {
        return indicatorRepository.findTopByCorpCodeOrderByBasDtDesc(stockCode)
                .map(mapper::toDto)
                .orElse(null);
    }
}
```

**파일 3**: `services/stock-finance/src/main/java/com/stock/finance/repository/CorpFinanceIndicatorRepository.java`

```java
@Repository
public interface CorpFinanceIndicatorRepository extends JpaRepository<CorpFinanceIndicator, CorpFinanceId> {
    List<CorpFinanceIndicator> findByCorpCodeInAndBasDt(List<String> corpCodes, LocalDate basDt);
    Optional<CorpFinanceIndicator> findTopByCorpCodeOrderByBasDtDesc(String corpCode);
}
```

**파일 4**: `services/stock-finance/build.gradle` (의존성 추가)

```gradle
dependencies {
    // 기존 의존성들...
    
    // MapStruct
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
}
```

**예상 시간**: 1일

---

### Step 2: stock-common에 DTO 추가

**파일 1**: `modules/stock-common/src/main/java/com/stock/common/dto/CorpFinanceIndicatorDto.java` (신규)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorpFinanceIndicatorDto {
    private String corpCode;
    private LocalDate basDt;
    private BigDecimal per;
    private BigDecimal pbr;
    private BigDecimal psr;
    private BigDecimal roe;
    private BigDecimal roa;
    private BigDecimal debtRatio;
    private BigDecimal revenueGrowth;
    private BigDecimal netIncomeGrowth;
    private BigDecimal opIncomeGrowth;
}
```

**파일 2**: `modules/stock-common/src/main/java/com/stock/common/dto/ValueStrategyConfig.java` (신규)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueStrategyConfig {
    
    /**
     * 선정할 상위 종목 수 (기본값: 20)
     */
    @Builder.Default
    private Integer topN = 20;
    
    /**
     * PER 가중치 (기본값: 0.3)
     */
    @Builder.Default
    private BigDecimal perWeight = new BigDecimal("0.3");
    
    /**
     * PBR 가중치 (기본값: 0.3)
     */
    @Builder.Default
    private BigDecimal pbrWeight = new BigDecimal("0.3");
    
    /**
     * ROE 가중치 (기본값: 0.4)
     */
    @Builder.Default
    private BigDecimal roeWeight = new BigDecimal("0.4");
    
    /**
     * 가중치 합계 검증
     */
    public void validate() {
        BigDecimal sum = perWeight.add(pbrWeight).add(roeWeight);
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(
                "Weight sum must be 1.0, but got: " + sum);
        }
    }
}
```

**예상 시간**: 0.5일

---

### Step 3: FinanceClient 구현

**파일**: `services/stock-strategy/src/main/java/com/stock/strategy/client/FinanceClient.java`

```java
@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestClient restClient;

    @Value("${services.stock-finance.url:http://localhost:8082}")
    private String financeServiceUrl;

    public List<CorpFinanceIndicatorDto> getIndicatorsBatch(List<String> stockCodes, String date) {
        String stockCodesParam = String.join(",", stockCodes);
        
        return restClient.get()
                .uri(financeServiceUrl + "/internal/indicators/batch?stockCodes=" 
                        + stockCodesParam + "&date=" + date)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CorpFinanceIndicatorDto>>() {});
    }

    public CorpFinanceIndicatorDto getLatestIndicator(String stockCode) {
        return restClient.get()
                .uri(financeServiceUrl + "/internal/indicators/" + stockCode + "/latest")
                .retrieve()
                .body(CorpFinanceIndicatorDto.class);
    }
}
```

**예상 시간**: 0.5일

---

### Step 4: ValueStrategy 구현

**파일**: `services/stock-strategy/src/main/java/com/stock/strategy/strategy/ValueStrategy.java` (신규)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ValueStrategy implements Strategy {

    private final PriceClient priceClient;
    private final FinanceClient financeClient;
    
    // 기본값
    private static final int DEFAULT_TOP_N = 20;
    private static final BigDecimal DEFAULT_WEIGHT_PER = new BigDecimal("0.3");
    private static final BigDecimal DEFAULT_WEIGHT_PBR = new BigDecimal("0.3");
    private static final BigDecimal DEFAULT_WEIGHT_ROE = new BigDecimal("0.4");

    @Override
    public String getName() {
        return "Value";
    }

    @Override
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, List<String> universe) {
        return rebalance(date, portfolio, universe, null);
    }
    
    /**
     * 가중치 설정 가능한 리밸런싱
     */
    public List<TradeOrder> rebalance(LocalDate date, Portfolio portfolio, 
                                      List<String> universe, ValueStrategyConfig config) {
        List<TradeOrder> orders = new ArrayList<>();

        if (universe.isEmpty()) {
            return orders;
        }

        // 설정값 또는 기본값 사용
        if (config == null) {
            config = ValueStrategyConfig.builder().build();
        }
        config.validate();
        
        int topN = config.getTopN();
        BigDecimal perWeight = config.getPerWeight();
        BigDecimal pbrWeight = config.getPbrWeight();
        BigDecimal roeWeight = config.getRoeWeight();

        try {
            // 1. 가치 스코어 계산
            Map<String, BigDecimal> valueScores = 
                    calculateValueScores(universe, date, perWeight, pbrWeight, roeWeight);
            
            // 2. 상위 N개 종목 선정
            List<String> topStocks = valueScores.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (topStocks.isEmpty()) {
                return orders;
            }

            // 3. 전체 자산 가치
            BigDecimal totalValue = portfolio.getTotalValue();
            BigDecimal targetValuePerStock = totalValue.divide(
                    BigDecimal.valueOf(topStocks.size()), 2, RoundingMode.HALF_UP);

            // 4. 기존 보유 종목 중 상위 N개에 없는 종목 매도
            for (String stockCode : new ArrayList<>(portfolio.getHoldings().keySet())) {
                if (!topStocks.contains(stockCode)) {
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    orders.add(TradeOrder.builder()
                            .stockCode(stockCode)
                            .orderType(OrderType.SELL)
                            .quantity(holding.getQuantity())
                            .price(holding.getCurrentPrice())
                            .orderDate(date)
                            .build());
                }
            }

            // 5. 상위 종목 리밸런싱
            String dateStr = DateUtils.toLocalDateString(date);
            for (String stockCode : topStocks) {
                try {
                    var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                    if (priceDto == null || priceDto.getEndPrice() == null) {
                        continue;
                    }

                    BigDecimal currentPrice = priceDto.getEndPrice();
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    BigDecimal currentValue = holding != null ? holding.getMarketValue() : BigDecimal.ZERO;
                    BigDecimal diff = targetValuePerStock.subtract(currentValue);
                    
                    if (diff.abs().compareTo(currentPrice) > 0) {
                        if (diff.compareTo(BigDecimal.ZERO) > 0) {
                            // 매수
                            int quantity = diff.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.BUY)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        } else {
                            // 매도
                            int quantity = diff.abs().divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0 && holding != null && holding.getQuantity() >= quantity) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.SELL)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get price for {}: {}", stockCode, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to rebalance with value strategy", e);
        }

        return orders;
    }

    private Map<String, BigDecimal> calculateValueScores(List<String> universe, LocalDate date,
                                                         BigDecimal perWeight, BigDecimal pbrWeight, 
                                                         BigDecimal roeWeight) {
        Map<String, BigDecimal> scores = new HashMap<>();
        String dateStr = date.toString();
        
        try {
            List<CorpFinanceIndicatorDto> indicators = financeClient.getIndicatorsBatch(universe, dateStr);
            
            for (CorpFinanceIndicatorDto indicator : indicators) {
                try {
                    BigDecimal score = calculateScore(indicator, perWeight, pbrWeight, roeWeight);
                    if (score != null) {
                        scores.put(indicator.getCorpCode(), score);
                    }
                } catch (Exception e) {
                    log.warn("Failed to calculate value score for {}: {}", 
                            indicator.getCorpCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch indicators batch", e);
        }

        return scores;
    }

    private BigDecimal calculateScore(CorpFinanceIndicatorDto indicator,
                                     BigDecimal perWeight, BigDecimal pbrWeight, BigDecimal roeWeight) {
        BigDecimal per = indicator.getPer();
        BigDecimal pbr = indicator.getPbr();
        BigDecimal roe = indicator.getRoe();

        // 필수 지표 검증
        if (per == null || pbr == null || roe == null) {
            return null;
        }

        // 음수/0 제외
        if (per.compareTo(BigDecimal.ZERO) <= 0 || 
            pbr.compareTo(BigDecimal.ZERO) <= 0 || 
            roe.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 가치 스코어 = (1/PER) * perWeight + (1/PBR) * pbrWeight + (ROE/100) * roeWeight
        BigDecimal perScore = BigDecimal.ONE.divide(per, 8, RoundingMode.HALF_UP).multiply(perWeight);
        BigDecimal pbrScore = BigDecimal.ONE.divide(pbr, 8, RoundingMode.HALF_UP).multiply(pbrWeight);
        BigDecimal roeScore = roe.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).multiply(roeWeight);

        return perScore.add(pbrScore).add(roeScore);
    }
}
```

**예상 시간**: 2일
                    orders.add(TradeOrder.builder()
                            .stockCode(stockCode)
                            .orderType(OrderType.SELL)
                            .quantity(holding.getQuantity())
                            .price(holding.getCurrentPrice())
                            .orderDate(date)
                            .build());
                }
            }

            // 5. 상위 종목 리밸런싱
            String dateStr = DateUtils.toLocalDateString(date);
            for (String stockCode : topStocks) {
                try {
                    var priceDto = priceClient.getPriceByDate(stockCode, dateStr);
                    if (priceDto == null || priceDto.getEndPrice() == null) {
                        continue;
                    }

                    BigDecimal currentPrice = priceDto.getEndPrice();
                    PortfolioHolding holding = portfolio.getHoldings().get(stockCode);
                    BigDecimal currentValue = holding != null ? holding.getMarketValue() : BigDecimal.ZERO;
                    BigDecimal diff = targetValuePerStock.subtract(currentValue);
                    
                    if (diff.abs().compareTo(currentPrice) > 0) {
                        if (diff.compareTo(BigDecimal.ZERO) > 0) {
                            // 매수
                            int quantity = diff.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.BUY)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        } else {
                            // 매도
                            int quantity = diff.abs().divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (quantity > 0 && holding != null && holding.getQuantity() >= quantity) {
                                orders.add(TradeOrder.builder()
                                        .stockCode(stockCode)
                                        .orderType(OrderType.SELL)
                                        .quantity(quantity)
                                        .price(currentPrice)
                                        .orderDate(date)
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get price for {}: {}", stockCode, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to rebalance with value strategy", e);
        }

        return orders;
    }

    private Map<String, BigDecimal> calculateValueScores(List<String> universe, LocalDate date) {
        Map<String, BigDecimal> scores = new HashMap<>();
        String dateStr = date.toString();
        
        try {
            List<CorpFinanceIndicatorDto> indicators = financeClient.getIndicatorsBatch(universe, dateStr);
            
            for (CorpFinanceIndicatorDto indicator : indicators) {
                try {
                    BigDecimal score = calculateScore(indicator);
                    if (score != null) {
                        scores.put(indicator.getCorpCode(), score);
                    }
                } catch (Exception e) {
                    log.warn("Failed to calculate value score for {}: {}", 
                            indicator.getCorpCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch indicators batch", e);
        }

        return scores;
    }

    private BigDecimal calculateScore(CorpFinanceIndicatorDto indicator) {
        BigDecimal per = indicator.getPer();
        BigDecimal pbr = indicator.getPbr();
        BigDecimal roe = indicator.getRoe();

        // 필수 지표 검증
        if (per == null || pbr == null || roe == null) {
            return null;
        }

        // 음수/0 제외
        if (per.compareTo(BigDecimal.ZERO) <= 0 || 
            pbr.compareTo(BigDecimal.ZERO) <= 0 || 
            roe.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 가치 스코어 = (1/PER) * 0.3 + (1/PBR) * 0.3 + (ROE/100) * 0.4
        BigDecimal perScore = BigDecimal.ONE.divide(per, 8, RoundingMode.HALF_UP).multiply(WEIGHT_PER);
        BigDecimal pbrScore = BigDecimal.ONE.divide(pbr, 8, RoundingMode.HALF_UP).multiply(WEIGHT_PBR);
        BigDecimal roeScore = roe.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP).multiply(WEIGHT_ROE);

        return perScore.add(pbrScore).add(roeScore);
    }
}
```

**예상 시간**: 2일

---

### Step 5: StrategyFactory 업데이트

**파일**: `services/stock-strategy/src/main/java/com/stock/strategy/service/StrategyFactory.java`

```java
@Service
@RequiredArgsConstructor
public class StrategyFactory {

    private final EqualWeightStrategy equalWeightStrategy;
    private final MomentumStrategy momentumStrategy;
    private final LowVolatilityStrategy lowVolatilityStrategy;
    private final ValueStrategy valueStrategy; // 추가

    public Strategy getStrategy(StrategyType strategyType) {
        return switch (strategyType) {
            case EQUAL_WEIGHT -> equalWeightStrategy;
            case MOMENTUM -> momentumStrategy;
            case LOW_VOLATILITY -> lowVolatilityStrategy;
            case VALUE -> valueStrategy; // 수정
        };
    }

    public Strategy getStrategy(String strategyName) {
        return getStrategy(StrategyType.fromCode(strategyName));
    }

    public List<StrategyType> getAvailableStrategies() {
        return Arrays.asList(StrategyType.values());
    }
}
```

**예상 시간**: 0.5일

---

### Step 6: 테스트 작성

**파일**: `services/stock-strategy/src/test/java/com/stock/strategy/strategy/ValueStrategyTest.java` (신규)

```java
@SpringBootTest
class ValueStrategyTest {

    @Autowired
    private ValueStrategy valueStrategy;

    @Test
    void testRebalance_withValidData_shouldReturnOrders() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        Portfolio portfolio = Portfolio.builder()
                .totalValue(new BigDecimal("10000000"))
                .cashBalance(new BigDecimal("10000000"))
                .holdings(new HashMap<>())
                .build();
        List<String> universe = List.of("005930", "000660", "035420");

        // When
        List<TradeOrder> orders = valueStrategy.rebalance(date, portfolio, universe);

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders).allMatch(order -> order.getOrderType() == OrderType.BUY);
    }

    @Test
    void testRebalance_withEmptyUniverse_shouldReturnEmptyOrders() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        Portfolio portfolio = Portfolio.builder()
                .totalValue(new BigDecimal("10000000"))
                .cashBalance(new BigDecimal("10000000"))
                .holdings(new HashMap<>())
                .build();
        List<String> universe = List.of();

        // When
        List<TradeOrder> orders = valueStrategy.rebalance(date, portfolio, universe);

        // Then
        assertThat(orders).isEmpty();
    }
}
```

**예상 시간**: 1일

---

## 📅 구현 일정

| 단계 | 작업 내용 | 예상 시간 |
|------|----------|----------|
| Step 0 | BacktestRequest 및 엔티티 수정 | 0.5일 |
| Step 1 | stock-finance 내부 API 추가 | 1일 |
| Step 2 | DTO 추가 (CorpFinanceIndicatorDto, ValueStrategyConfig) | 0.5일 |
| Step 3 | FinanceClient 구현 | 0.5일 |
| Step 4 | ValueStrategy 구현 (가중치 설정 지원) | 2일 |
| Step 5 | StrategyFactory 업데이트 | 0.5일 |
| Step 6 | 테스트 작성 | 1일 |

**총 예상 시간**: 6일

---

## ✅ 완료 기준

- [ ] `BacktestRequest`에 `valueStrategyConfig` 필드 추가
- [ ] `BacktestSimulation` 엔티티에 JSON 설정 저장 필드 추가
- [ ] stock-finance 서비스에 `/internal/indicators/batch` API 추가
- [ ] stock-finance 서비스에 `/internal/indicators/{stockCode}/latest` API 추가
- [ ] `CorpFinanceIndicatorDto` 생성
- [ ] `ValueStrategyConfig` 생성 (가중치 검증 포함)
- [ ] `FinanceClient` 구현
- [ ] `ValueStrategy` 구현 (기본값 및 커스텀 가중치 지원)
- [ ] `StrategyFactory`에 VALUE 전략 등록
- [ ] 단위 테스트 작성 및 통과
- [ ] 기본 가중치 백테스팅 실행 및 검증
- [ ] 커스텀 가중치 백테스팅 실행 및 검증

---

## ⚠️ 주의사항

### 1. Stock Code 형식
- 기업 정보: `A900100` (A 접두사)
- 주가/재무 데이터: `900100` (숫자만)
- **ValueStrategy에서는 숫자만 사용**

### 2. 데이터 정밀도
- 모든 금융 계산은 `BigDecimal` 사용
- `RoundingMode.HALF_UP` 적용
- 정밀도: 8자리

### 3. 성능 최적화
- **배치 조회 필수**: `getIndicatorsBatch()` 사용
- N+1 문제 방지
- 캐싱 고려 (향후)

### 4. 에러 핸들링
- 재무 지표가 없는 종목은 제외
- 음수/0 값 처리
- 로그 기록

### 5. 가중치 설정
- **가중치 합계는 반드시 1.0이어야 함** (검증 로직 포함)
- 미입력 시 기본값 사용: PER 30%, PBR 30%, ROE 40%
- 커스텀 가중치 예시:
  - 보수적 전략: PER 50%, PBR 40%, ROE 10% (저평가 중시)
  - 성장 중시: PER 20%, PBR 20%, ROE 60% (수익성 중시)
  - 균형 전략: PER 33%, PBR 33%, ROE 34% (균등 배분)

---

## 🔍 검증 방법

### 1. 단위 테스트
```bash
./gradlew :services:stock-strategy:test --tests ValueStrategyTest
```

### 2. 통합 테스트 (백테스팅 실행)

**기본 가중치 사용 (PER 30%, PBR 30%, ROE 40%)**:
```bash
curl -X POST http://localhost:8080/api/v1/strategy/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "VALUE",
    "startDate": "2023-01-01",
    "endDate": "2023-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "tradingFeeRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI",
      "minMarketCap": 100000000000
    }
  }'
```

**커스텀 가중치 사용 (PER 50%, PBR 20%, ROE 30%, 상위 30개 종목)**:
```bash
curl -X POST http://localhost:8080/api/v1/strategy/backtest \
  -H "Content-Type: application/json" \
  -d '{
    "strategyType": "VALUE",
    "startDate": "2023-01-01",
    "endDate": "2023-12-31",
    "initialCapital": 10000000,
    "rebalancingPeriod": "MONTHLY",
    "tradingFeeRate": 0.00015,
    "taxRate": 0.0023,
    "universeFilter": {
      "market": "KOSPI",
      "minMarketCap": 100000000000
    },
    "valueStrategyConfig": {
      "topN": 30,
      "perWeight": 0.5,
      "pbrWeight": 0.2,
      "roeWeight": 0.3
    }
  }'
```

### 3. 결과 조회
```bash
curl http://localhost:8080/api/v1/strategy/backtest/{simulationId}/result
```

---

## 📚 참고 문서

- [Phase 2 백테스팅 엔진 구축 계획](./Phase2_Backtesting_Engine_Plan.md)
- [Coding Guidelines](./.kiro/steering/guidelines.md)
- [프로젝트 구조](./.kiro/steering/structure.md)
