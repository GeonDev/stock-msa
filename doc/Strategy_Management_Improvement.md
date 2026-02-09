# 전략 관리 개선 방안 (Strategy Management Improvement)

## 현재 문제점

### 1. 하드코딩된 전략 선택
```java
public Strategy getStrategy(String strategyName) {
    return switch (strategyName.toLowerCase()) {
        case "equalweight" -> equalWeightStrategy;
        case "momentum" -> momentumStrategy;
        case "lowvolatility" -> lowVolatilityStrategy;
        default -> throw new IllegalArgumentException("Unknown strategy: " + strategyName);
    };
}
```

**문제**:
- 문자열 기반으로 타입 안정성 부족
- 새 전략 추가 시 코드 수정 필요
- 오타 가능성
- 확장성 제한

---

## 개선 방안

### 방안 1: Enum 기반 전략 관리 (단순, 빠른 구현)

#### 장점
- ✅ 타입 안정성 확보
- ✅ 컴파일 타임 검증
- ✅ IDE 자동완성 지원
- ✅ 구현 간단

#### 단점
- ❌ 새 전략 추가 시 코드 수정 필요
- ❌ 런타임 동적 추가 불가

#### 구현 예시

**1) StrategyType Enum 생성**
```java
// services/stock-strategy/src/main/java/com/stock/strategy/enums/StrategyType.java
package com.stock.strategy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StrategyType {
    EQUAL_WEIGHT("EqualWeight", "동일 비중 전략"),
    MOMENTUM("Momentum", "모멘텀 전략"),
    LOW_VOLATILITY("LowVolatility", "저변동성 전략"),
    VALUE("Value", "가치 투자 전략");

    private final String code;
    private final String description;

    public static StrategyType fromCode(String code) {
        for (StrategyType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown strategy code: " + code);
    }
}
```

**2) StrategyFactory 수정**
```java
@Service
@RequiredArgsConstructor
public class StrategyFactory {

    private final EqualWeightStrategy equalWeightStrategy;
    private final MomentumStrategy momentumStrategy;
    private final LowVolatilityStrategy lowVolatilityStrategy;
    // private final ValueStrategy valueStrategy;

    public Strategy getStrategy(StrategyType strategyType) {
        return switch (strategyType) {
            case EQUAL_WEIGHT -> equalWeightStrategy;
            case MOMENTUM -> momentumStrategy;
            case LOW_VOLATILITY -> lowVolatilityStrategy;
            case VALUE -> throw new UnsupportedOperationException("Value strategy not implemented yet");
        };
    }

    // 하위 호환성을 위한 메서드
    public Strategy getStrategy(String strategyName) {
        return getStrategy(StrategyType.fromCode(strategyName));
    }

    public List<StrategyType> getAvailableStrategies() {
        return Arrays.asList(StrategyType.values());
    }
}
```

**3) API 요청 DTO 수정**
```java
@Data
public class BacktestRequest {
    @NotNull
    private StrategyType strategyType;  // String -> StrategyType
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    // ... 기타 필드
}
```

**4) 사용 예시**
```java
// Controller
Strategy strategy = strategyFactory.getStrategy(request.getStrategyType());

// JSON 요청
{
  "strategyType": "MOMENTUM",  // 또는 "Momentum"
  "startDate": "2020-01-01",
  "endDate": "2023-12-31"
}
```

---

### 방안 2: DB 기반 동적 전략 관리 (메타데이터 관리)

#### ⚠️ 중요: 오해하기 쉬운 점
**새로운 전략 클래스 추가 시 빌드는 여전히 필요합니다!**
- 새 Strategy 구현체 작성 → 빌드 → 배포 (Enum과 동일)
- DB는 단지 "이미 배포된 전략들"의 메타데이터를 관리하는 역할

#### 진짜 장점 (Enum 대비)
- ✅ **활성화/비활성화 제어** (재배포 없이)
- ✅ **전략별 메타데이터 관리** (설명, 파라미터, 제약사항 등)
- ✅ **전략별 설정 변경** (재배포 없이)
- ✅ **사용 통계 및 이력 관리**
- ✅ **A/B 테스트 및 점진적 롤아웃**

#### 단점
- ❌ 구현 복잡도 증가
- ❌ DB 의존성 추가
- ❌ 성능 오버헤드 (캐싱으로 해결 가능)
- ❌ **새 전략 추가 시 여전히 빌드 필요** (Enum과 동일)

#### 구현 예시

**1) 전략 메타데이터 테이블**
```sql
-- V2__add_strategy_metadata.sql
CREATE TABLE TB_STRATEGY_METADATA (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_code VARCHAR(50) NOT NULL UNIQUE,
    strategy_name VARCHAR(100) NOT NULL,
    description TEXT,
    bean_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_strategy_code (strategy_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 초기 데이터
INSERT INTO TB_STRATEGY_METADATA (strategy_code, strategy_name, description, bean_name) VALUES
('EQUAL_WEIGHT', 'EqualWeight', '유니버스 내 모든 종목에 동일 비중 배분', 'equalWeightStrategy'),
('MOMENTUM', 'Momentum', '과거 수익률 상위 종목 선정', 'momentumStrategy'),
('LOW_VOLATILITY', 'LowVolatility', '변동성 하위 종목 선정', 'lowVolatilityStrategy');
```

**2) Entity 생성**
```java
@Entity
@Data
@Table(name = "TB_STRATEGY_METADATA")
public class StrategyMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "strategy_code", unique = true, nullable = false)
    private String strategyCode;
    
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "bean_name", nullable = false)
    private String beanName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**3) Repository**
```java
@Repository
public interface StrategyMetadataRepository extends JpaRepository<StrategyMetadata, Long> {
    Optional<StrategyMetadata> findByStrategyCodeAndIsActiveTrue(String strategyCode);
    List<StrategyMetadata> findAllByIsActiveTrue();
}
```

**4) StrategyFactory 수정**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyFactory {

    private final ApplicationContext applicationContext;
    private final StrategyMetadataRepository strategyMetadataRepository;
    
    // 캐싱으로 성능 최적화
    private final Map<String, Strategy> strategyCache = new ConcurrentHashMap<>();

    public Strategy getStrategy(String strategyCode) {
        // 캐시 확인
        if (strategyCache.containsKey(strategyCode)) {
            return strategyCache.get(strategyCode);
        }

        // DB에서 메타데이터 조회
        StrategyMetadata metadata = strategyMetadataRepository
                .findByStrategyCodeAndIsActiveTrue(strategyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive strategy: " + strategyCode));

        // Spring Bean 조회
        try {
            Strategy strategy = (Strategy) applicationContext.getBean(metadata.getBeanName());
            strategyCache.put(strategyCode, strategy);
            return strategy;
        } catch (Exception e) {
            log.error("Failed to load strategy bean: {}", metadata.getBeanName(), e);
            throw new IllegalStateException("Strategy bean not found: " + metadata.getBeanName());
        }
    }

    public List<StrategyMetadata> getAvailableStrategies() {
        return strategyMetadataRepository.findAllByIsActiveTrue();
    }

    // 캐시 갱신 (새 전략 추가 시)
    public void refreshCache() {
        strategyCache.clear();
    }
}
```

**5) 전략 Bean 이름 명시**
```java
@Component("equalWeightStrategy")  // Bean 이름 명시
public class EqualWeightStrategy implements Strategy {
    // ...
}

@Component("momentumStrategy")
public class MomentumStrategy implements Strategy {
    // ...
}

@Component("lowVolatilityStrategy")
public class LowVolatilityStrategy implements Strategy {
    // ...
}
```

**6) 사용 예시**
```java
// Controller
Strategy strategy = strategyFactory.getStrategy(request.getStrategyCode());

// JSON 요청
{
  "strategyCode": "MOMENTUM",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31"
}

// 사용 가능한 전략 조회
GET /api/v1/strategy/available
Response:
[
  {
    "strategyCode": "EQUAL_WEIGHT",
    "strategyName": "EqualWeight",
    "description": "유니버스 내 모든 종목에 동일 비중 배분",
    "isActive": true
  },
  {
    "strategyCode": "MOMENTUM",
    "strategyName": "Momentum",
    "description": "과거 수익률 상위 종목 선정",
    "isActive": true
  }
]
```

**7) 관리 API 추가 (선택)**
```java
@RestController
@RequestMapping("/api/v1/strategy/admin")
@RequiredArgsConstructor
public class StrategyAdminController {

    private final StrategyMetadataRepository strategyMetadataRepository;
    private final StrategyFactory strategyFactory;

    // 전략 활성화/비활성화
    @PatchMapping("/{strategyCode}/toggle")
    public ResponseEntity<Void> toggleStrategy(@PathVariable String strategyCode) {
        StrategyMetadata metadata = strategyMetadataRepository
                .findByStrategyCode(strategyCode)
                .orElseThrow(() -> new NotFoundException("Strategy not found"));
        
        metadata.setIsActive(!metadata.getIsActive());
        strategyMetadataRepository.save(metadata);
        strategyFactory.refreshCache();
        
        return ResponseEntity.ok().build();
    }

    // 새 전략 등록
    @PostMapping
    public ResponseEntity<StrategyMetadata> registerStrategy(@RequestBody StrategyMetadata metadata) {
        StrategyMetadata saved = strategyMetadataRepository.save(metadata);
        strategyFactory.refreshCache();
        return ResponseEntity.ok(saved);
    }
}
```

---

### 방안 3: 하이브리드 (Enum + DB)

#### 개념
- Enum으로 기본 전략 정의 (타입 안정성)
- DB에 추가 메타데이터 저장 (확장성)

#### 구현 예시

**1) Enum 정의**
```java
@Getter
@RequiredArgsConstructor
public enum StrategyType {
    EQUAL_WEIGHT("equalWeightStrategy"),
    MOMENTUM("momentumStrategy"),
    LOW_VOLATILITY("lowVolatilityStrategy"),
    VALUE("valueStrategy");

    private final String beanName;
}
```

**2) DB 메타데이터 (선택적 정보)**
```sql
CREATE TABLE TB_STRATEGY_CONFIG (
    strategy_type VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    default_params JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**3) StrategyFactory**
```java
@Service
@RequiredArgsConstructor
public class StrategyFactory {

    private final ApplicationContext applicationContext;
    private final StrategyConfigRepository configRepository;

    public Strategy getStrategy(StrategyType strategyType) {
        // DB에서 활성화 여부 확인
        StrategyConfig config = configRepository.findById(strategyType.name())
                .orElseThrow(() -> new IllegalArgumentException("Strategy not configured"));
        
        if (!config.getIsActive()) {
            throw new IllegalStateException("Strategy is disabled: " + strategyType);
        }

        // Bean 조회
        return (Strategy) applicationContext.getBean(strategyType.getBeanName());
    }
}
```

---

## 권장 사항

### Phase 2 현재 단계: **방안 1 (Enum 기반)** 권장
**이유**:
- 빠른 구현 (30분 이내)
- 타입 안정성 확보
- 현재 4개 전략으로 충분히 관리 가능
- 복잡도 최소화
- **새 전략 추가는 어차피 빌드 필요하므로 Enum으로 충분**

### Phase 3 이후: **방안 2 (DB 기반)** 또는 **방안 3 (하이브리드)** 고려
**고려 시점**:
- ✅ 전략 활성화/비활성화를 재배포 없이 제어해야 할 때
- ✅ 전략별 상세 메타데이터(설명, 제약사항, 권장 파라미터) 관리 필요 시
- ✅ 전략별 설정을 DB에서 동적으로 변경해야 할 때
- ✅ 전략 사용 통계 및 이력 추적 필요 시
- ✅ A/B 테스트 또는 점진적 롤아웃 필요 시

**주의**: DB 기반도 새 전략 클래스 추가 시 빌드는 필수입니다!

---

## 마이그레이션 가이드

### Enum 기반으로 전환 (방안 1)

**Step 1**: StrategyType Enum 생성
```bash
# 파일 생성
services/stock-strategy/src/main/java/com/stock/strategy/enums/StrategyType.java
```

**Step 2**: StrategyFactory 수정
```java
// String -> StrategyType 변경
public Strategy getStrategy(StrategyType strategyType)
```

**Step 3**: BacktestRequest DTO 수정
```java
private StrategyType strategyType;  // String strategyName 대체
```

**Step 4**: 기존 API 하위 호환성 유지 (선택)
```java
// 문자열도 받을 수 있도록
public Strategy getStrategy(String strategyName) {
    return getStrategy(StrategyType.fromCode(strategyName));
}
```

**Step 5**: 테스트 및 배포

---

## 비교 표

| 항목 | 현재 (String) | Enum | DB 기반 | 하이브리드 |
|------|--------------|------|---------|-----------|
| 타입 안정성 | ❌ | ✅ | ⚠️ | ✅ |
| 컴파일 검증 | ❌ | ✅ | ❌ | ✅ |
| **새 전략 추가 시 빌드** | **✅ 필요** | **✅ 필요** | **✅ 필요** | **✅ 필요** |
| 활성화/비활성화 (재배포 없이) | ❌ | ❌ | ✅ | ✅ |
| 메타데이터 관리 | ❌ | ❌ | ✅ | ✅ |
| 전략별 설정 변경 (재배포 없이) | ❌ | ❌ | ✅ | ✅ |
| 구현 복잡도 | 낮음 | 낮음 | 높음 | 중간 |
| 성능 | 빠름 | 빠름 | 보통 (캐싱 필요) | 빠름 |
| 유지보수성 | 낮음 | 중간 | 높음 | 높음 |
| 권장 시점 | - | Phase 2 | Phase 3+ | Phase 4+ |

**핵심**: 모든 방안에서 새 전략 클래스 추가 시 빌드는 필수입니다!

---

## 결론

**즉시 적용 (Phase 2)**: Enum 기반 (방안 1)
- 최소한의 코드 변경
- 타입 안정성 확보
- 빠른 구현
- **새 전략 추가는 어차피 빌드가 필요하므로 Enum으로 충분**

**향후 고려 (Phase 3+)**: DB 기반 (방안 2)
- **재배포 없이** 전략 활성화/비활성화 제어 필요 시
- 전략별 메타데이터 및 설정 관리 필요 시
- A/B 테스트 및 점진적 롤아웃 필요 시

### 핵심 정리
모든 방안에서 **새로운 전략 클래스를 추가하려면 코드 작성 → 빌드 → 배포**가 필요합니다.

DB 기반의 장점은:
- "이미 배포된 전략들"을 재배포 없이 제어
- 메타데이터 및 설정 관리
- 운영 유연성 확보

단순히 전략 선택의 타입 안정성만 필요하다면 **Enum이 최선**입니다.
