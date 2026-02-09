# Enum 기반 전략 관리 전환 완료

## 변경 일자
2026-02-09

## 변경 내용

### 1. StrategyType Enum 생성 ✅
**파일**: `services/stock-strategy/src/main/java/com/stock/strategy/enums/StrategyType.java`

```java
public enum StrategyType {
    EQUAL_WEIGHT("EqualWeight", "동일 비중 전략"),
    MOMENTUM("Momentum", "모멘텀 전략"),
    LOW_VOLATILITY("LowVolatility", "저변동성 전략"),
    VALUE("Value", "가치 투자 전략");
}
```

**특징**:
- 타입 안정성 확보
- 컴파일 타임 검증
- `fromCode()` 메서드로 문자열 변환 지원

### 2. StrategyFactory 수정 ✅
**변경 전**:
```java
public Strategy getStrategy(String strategyName) {
    return switch (strategyName.toLowerCase()) {
        case "equalweight" -> equalWeightStrategy;
        // ...
    };
}
```

**변경 후**:
```java
public Strategy getStrategy(StrategyType strategyType) {
    return switch (strategyType) {
        case EQUAL_WEIGHT -> equalWeightStrategy;
        case MOMENTUM -> momentumStrategy;
        case LOW_VOLATILITY -> lowVolatilityStrategy;
        case VALUE -> throw new UnsupportedOperationException("Value strategy not implemented yet");
    };
}

// 하위 호환성 유지
public Strategy getStrategy(String strategyName) {
    return getStrategy(StrategyType.fromCode(strategyName));
}
```

### 3. BacktestRequest DTO 수정 ✅
**변경 전**:
```java
private String strategyName;
```

**변경 후**:
```java
private StrategyType strategyType;
```

### 4. 관련 서비스 수정 ✅
- `BacktestService.java`: `request.getStrategyType().getCode()` 사용
- `SimulationEngine.java`: `request.getStrategyType()` 사용

## 사용 방법

### API 요청 (JSON)
```json
{
  "strategyType": "MOMENTUM",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000000,
  "rebalancingPeriod": "MONTHLY"
}
```

**허용되는 값**:
- `EQUAL_WEIGHT` 또는 `EqualWeight`
- `MOMENTUM` 또는 `Momentum`
- `LOW_VOLATILITY` 또는 `LowVolatility`
- `VALUE` 또는 `Value` (미구현)

### 코드에서 사용
```java
// Enum 직접 사용
Strategy strategy = strategyFactory.getStrategy(StrategyType.MOMENTUM);

// 문자열 변환 (하위 호환)
Strategy strategy = strategyFactory.getStrategy("Momentum");

// 사용 가능한 전략 목록
List<StrategyType> strategies = strategyFactory.getAvailableStrategies();
```

## 장점

### 1. 타입 안정성
```java
// 컴파일 에러 발생 (오타 방지)
strategyFactory.getStrategy(StrategyType.MOMENTM);  // ❌

// 정상 작동
strategyFactory.getStrategy(StrategyType.MOMENTUM);  // ✅
```

### 2. IDE 지원
- 자동완성 지원
- 리팩토링 안전성
- 사용처 추적 용이

### 3. 명확한 계약
```java
public Strategy getStrategy(StrategyType strategyType)  // 명확한 타입
vs
public Strategy getStrategy(String strategyName)        // 모호한 타입
```

### 4. 하위 호환성
기존 문자열 기반 API도 계속 작동:
```java
strategyFactory.getStrategy("Momentum")  // ✅ 여전히 작동
```

## 테스트 결과

### 컴파일 테스트
```bash
./gradlew :services:stock-strategy:compileJava
```
✅ **BUILD SUCCESSFUL**

## 마이그레이션 가이드

### 기존 코드 수정
**Before**:
```java
BacktestRequest request = BacktestRequest.builder()
    .strategyName("Momentum")
    .build();
```

**After**:
```java
BacktestRequest request = BacktestRequest.builder()
    .strategyType(StrategyType.MOMENTUM)
    .build();
```

### JSON 요청 수정
**Before**:
```json
{
  "strategyName": "Momentum"
}
```

**After**:
```json
{
  "strategyType": "MOMENTUM"
}
```

또는 (대소문자 무관):
```json
{
  "strategyType": "Momentum"
}
```

## 향후 전략 추가 방법

### 1. Enum에 추가
```java
public enum StrategyType {
    // 기존 전략들...
    NEW_STRATEGY("NewStrategy", "새로운 전략 설명");
}
```

### 2. 전략 클래스 구현
```java
@Component
public class NewStrategy implements Strategy {
    // 구현...
}
```

### 3. StrategyFactory에 등록
```java
private final NewStrategy newStrategy;

public Strategy getStrategy(StrategyType strategyType) {
    return switch (strategyType) {
        // 기존 케이스들...
        case NEW_STRATEGY -> newStrategy;
    };
}
```

## 결론

String 기반에서 Enum 기반으로 성공적으로 전환되었습니다.

**개선 사항**:
- ✅ 타입 안정성 확보
- ✅ 컴파일 타임 검증
- ✅ IDE 지원 강화
- ✅ 하위 호환성 유지
- ✅ 코드 가독성 향상
