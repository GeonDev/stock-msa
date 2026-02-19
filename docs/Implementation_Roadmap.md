# 구현 계획 (Roadmap)

> **목적**: Stock-MSA 시스템의 향후 개발 방향 및 우선순위

## 📅 전체 로드맵

```
Phase 0: 데이터 수집 인프라 ✅ (완료)
Phase 1: 데이터 품질 검증 🚧 (85% 완료)
Phase 2: 백테스팅 엔진 🚧 (70% 완료)
Phase 3: 고급 전략 ⏳ (계획)
Phase 4: 실시간 시스템 ⏳ (계획)
Phase 5: 프로덕션 준비 ⏳ (계획)
```

---

## Phase 1: 데이터 품질 검증 (진행중)

**목표**: 수집된 데이터의 정합성 및 완전성 확보

### 1.1 데이터 정합성 검증 ⏳
**우선순위**: 높음  
**예상 기간**: 1주

**작업 내용**:
- [ ] 시계열 데이터 연속성 확인
  - 주가 데이터 누락일 탐지
  - 재무 데이터 분기별 연속성
- [ ] 이상치 탐지
  - 극단적 비율 (PER > 1000, PBR < 0)
  - 급격한 변동 (일일 변동률 > 30%)
- [ ] 중복 데이터 제거
  - 동일 날짜 중복 주가
  - 동일 분기 중복 재무제표
- [ ] 데이터 보정 로직
  - 자동 보정 규칙 정의
  - 수동 검토 프로세스

**구현 계획**:
```java
public interface DataValidator {
    ValidationResult validateTimeSeries(List<StockPrice> prices);
    List<Outlier> detectOutliers(List<CorpFinance> finances);
    List<Duplicate> findDuplicates(String stockCode, LocalDate date);
    void correctData(String stockCode, LocalDate date, CorrectionRule rule);
}
```

### 1.2 성능 모니터링 ⏳
**우선순위**: 중간  
**예상 기간**: 1주

**작업 내용**:
- [ ] 배치 처리 시간 측정
  - 각 단계별 소요 시간
  - 병목 구간 식별
- [ ] API 호출 횟수 추적
  - DART API 사용량 모니터링
  - 일일 제한 근접 알림
- [ ] 메모리 사용량 모니터링
  - 힙 메모리 사용 추이
  - GC 빈도 및 시간
- [ ] 데이터베이스 쿼리 최적화
  - 슬로우 쿼리 로그
  - 인덱스 최적화

**구현 계획**:
```java
@Component
public class PerformanceMonitor {
    public void recordBatchTime(String batchName, Duration duration);
    public void recordApiCall(String apiName, int count);
    public void recordMemoryUsage(long heapUsed, long heapMax);
    public void recordSlowQuery(String query, Duration duration);
}
```

### 1.3 데이터 보완 로직 ⏳
**우선순위**: 중간  
**예상 기간**: 2주

**작업 내용**:
- [ ] 주가 데이터 보완
  - 누락일 데이터 재수집
  - 이전/다음 거래일 데이터로 보간
- [ ] 재무 데이터 보완
  - 실패 건 재시도 로직
  - 대체 데이터 소스 활용
- [ ] DART Corp Code 매핑 개선
  - 매핑 실패 종목 수동 매핑
  - 매핑 정확도 향상 (95%+ 목표)
- [ ] 기술적 지표 계산 개선
  - 히스토리 부족 종목 처리
  - 지표 계산 성공률 향상 (95%+ 목표)

**참고**:
- 현재 주가 수집 시 수정주가 및 기술적 지표가 자동 계산됨
- 별도 API 호출 불필요 (통합 배치 처리)
- 기술적 지표는 300일 이상 히스토리 필요

---

## Phase 2: 백테스팅 엔진 (진행중)

**목표**: 전략 검증 및 성과 분석 완성

### 2.1 리스크 지표 구현 ⏳
**우선순위**: 높음  
**예상 기간**: 1주

**작업 내용**:
- [ ] 변동성 (Volatility)
  - 일별 수익률 표준편차
  - 연환산 변동성
- [ ] 최대 낙폭 (MDD)
  - 고점 대비 최대 하락률
  - 회복 기간
- [ ] 샤프 비율 (Sharpe Ratio)
  - (수익률 - 무위험 수익률) / 변동성
  - 연환산 샤프 비율
- [ ] 소르티노 비율 (Sortino Ratio)
  - 하방 리스크만 고려
  - 상방 변동성 제외

**구현 계획**:
```java
public class RiskMetrics {
    public BigDecimal calculateVolatility(List<BigDecimal> returns);
    public BigDecimal calculateMDD(List<BigDecimal> portfolioValues);
    public BigDecimal calculateSharpeRatio(
        BigDecimal avgReturn, 
        BigDecimal volatility, 
        BigDecimal riskFreeRate
    );
    public BigDecimal calculateSortinoRatio(
        BigDecimal avgReturn, 
        BigDecimal downside Volatility, 
        BigDecimal riskFreeRate
    );
}
```

### 2.2 전략 관리 개선 ⏳
**우선순위**: 중간  
**예상 기간**: 2주

**작업 내용**:
- [ ] 전략 저장 및 불러오기
  - 전략 설정 JSON 직렬화
  - 데이터베이스 저장
- [ ] 전략 비교 기능
  - 여러 전략 동시 실행
  - 성과 비교 테이블
- [ ] 전략 최적화
  - 파라미터 그리드 서치
  - 최적 파라미터 자동 탐색
- [ ] 전략 조합 (앙상블)
  - 여러 전략 가중 평균
  - 동적 가중치 조정

**데이터베이스 스키마**:
```sql
TB_STRATEGY_CONFIG:
  - strategy_id (PK)
  - strategy_name
  - strategy_type
  - parameters (JSON)
  - created_at

TB_STRATEGY_COMPARISON:
  - comparison_id (PK)
  - strategy_ids (JSON array)
  - start_date, end_date
  - results (JSON)
```

### 2.3 거래 비용 반영 ⏳
**우선순위**: 높음  
**예상 기간**: 1주

**작업 내용**:
- [ ] 거래 수수료
  - 매수/매도 수수료율 설정
  - 최소 수수료 설정
- [ ] 세금
  - 증권거래세 (0.23%)
  - 농어촌특별세 (0.15%)
- [ ] 슬리피지 (Slippage)
  - 시장가 주문 시 가격 차이
  - 거래량 대비 슬리피지 모델
- [ ] 거래 제약
  - 최소 거래 단위 (1주)
  - 단일 종목 최대 비중

**구현 계획**:
```java
public class TradingCost {
    public BigDecimal calculateCommission(BigDecimal amount);
    public BigDecimal calculateTax(BigDecimal amount);
    public BigDecimal calculateSlippage(
        BigDecimal price, 
        long volume, 
        long avgVolume
    );
}
```

---

## Phase 3: 고급 전략 (계획)

**목표**: 다양한 퀀트 전략 구현

### 3.1 멀티팩터 전략 ⏳
**우선순위**: 높음  
**예상 기간**: 2주

**작업 내용**:
- [ ] 팩터 정의
  - Value: PER, PBR, PSR, PCR
  - Quality: ROE, ROA, 부채비율
  - Momentum: 과거 수익률
  - Size: 시가총액
  - Volatility: 변동성
- [ ] 팩터 스코어링
  - Z-Score 정규화
  - 백분위 순위
- [ ] 팩터 조합
  - 동일 가중
  - 최적 가중 (회귀 분석)
- [ ] 팩터 리밸런싱
  - 월간, 분기별

**구현 계획**:
```java
public interface Factor {
    BigDecimal calculateScore(StockData stock);
    String getName();
}

public class MultiFactorStrategy implements Strategy {
    private List<Factor> factors;
    private Map<Factor, BigDecimal> weights;
    
    public BigDecimal calculateCompositeScore(StockData stock) {
        return factors.stream()
            .map(f -> f.calculateScore(stock).multiply(weights.get(f)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### 3.2 섹터 로테이션 ⏳
**우선순위**: 중간  
**예상 기간**: 2주

**작업 내용**:
- [ ] 섹터 분류
  - GICS 섹터 매핑
  - 한국 표준산업분류 활용
- [ ] 섹터 모멘텀
  - 섹터별 평균 수익률
  - 상대 강도
- [ ] 섹터 선택
  - 상위 N개 섹터 선택
  - 섹터 내 종목 선택
- [ ] 리밸런싱
  - 월간, 분기별

### 3.3 동적 자산배분 ⏳
**우선순위**: 높음  
**예상 기간**: 3주

**작업 내용**:
- [ ] 자산군 정의
  - 주식 (KOSPI, KOSDAQ)
  - 채권 (국고채, 회사채)
  - 현금
- [ ] 배분 전략
  - 듀얼 모멘텀
  - 변동성 타겟팅
  - 리스크 패리티
- [ ] 리밸런싱
  - 월간, 분기별
  - 임계값 기반 (5% 이탈 시)

### 3.4 리스크 패리티 ⏳
**우선순위**: 중간  
**예상 기간**: 2주

**작업 내용**:
- [ ] 리스크 기여도 계산
  - 각 자산의 변동성 기여도
- [ ] 동일 리스크 배분
  - 모든 자산이 동일한 리스크 기여
- [ ] 레버리지 조정
  - 목표 변동성 달성
- [ ] 리밸런싱
  - 월간, 분기별

---

## Phase 4: 실시간 시스템 (계획)

**목표**: 실시간 데이터 수집 및 자동 매매

### 4.1 실시간 데이터 수집 ⏳
**우선순위**: 높음  
**예상 기간**: 4주

**작업 내용**:
- [ ] WebSocket 연동
  - 한국투자증권 API
  - 키움증권 API
- [ ] 실시간 주가
  - 체결가, 호가
  - 거래량
- [ ] 실시간 지표 계산
  - 기술적 지표 업데이트
  - 재무 지표 업데이트
- [ ] 이벤트 스트리밍
  - Kafka 도입
  - 실시간 알림

### 4.2 자동 매매 연동 ⏳
**우선순위**: 높음  
**예상 기간**: 4주

**작업 내용**:
- [ ] 증권사 API 연동
  - 한국투자증권
  - 키움증권
- [ ] 주문 실행
  - 시장가, 지정가 주문
  - 조건부 주문
- [ ] 주문 관리
  - 주문 상태 추적
  - 체결 확인
- [ ] 포지션 관리
  - 실시간 포트폴리오 조회
  - 손익 계산

### 4.3 리스크 관리 ⏳
**우선순위**: 높음  
**예상 기간**: 2주

**작업 내용**:
- [ ] 손절/익절
  - 자동 손절 주문
  - 목표가 도달 시 익절
- [ ] 포지션 제한
  - 단일 종목 최대 비중
  - 섹터별 최대 비중
- [ ] 일일 손실 제한
  - 일일 최대 손실률 설정
  - 제한 도달 시 거래 중단
- [ ] 알림
  - 주요 이벤트 알림
  - 리스크 경고

---

## Phase 5: 프로덕션 준비 (계획)

**목표**: 안정적인 운영 환경 구축

### 5.1 모니터링 및 로깅 ⏳
**우선순위**: 높음  
**예상 기간**: 2주

**작업 내용**:
- [ ] 중앙 로깅
  - ELK Stack (Elasticsearch, Logstash, Kibana)
  - 로그 수집 및 분석
- [ ] 메트릭 수집
  - Prometheus
  - Grafana 대시보드
- [ ] 알림
  - Slack, Email 연동
  - 장애 알림
- [ ] 헬스 체크
  - 서비스 상태 모니터링
  - 자동 재시작

### 5.2 보안 강화 ⏳
**우선순위**: 높음  
**예상 기간**: 2주

**작업 내용**:
- [ ] 인증/인가
  - JWT 토큰 기반 인증
  - 역할 기반 접근 제어 (RBAC)
- [ ] API 보안
  - Rate Limiting
  - API 키 관리
- [ ] 데이터 암호화
  - 민감 정보 암호화
  - HTTPS 적용
- [ ] 감사 로그
  - 모든 API 호출 기록
  - 데이터 변경 이력

### 5.3 성능 최적화 ⏳
**우선순위**: 중간  
**예상 기간**: 2주

**작업 내용**:
- [ ] 캐싱
  - Redis 도입
  - 자주 조회되는 데이터 캐싱
- [ ] 데이터베이스 최적화
  - 인덱스 최적화
  - 쿼리 튜닝
  - 파티셔닝
- [ ] 비동기 처리
  - 배치 작업 비동기화
  - 메시지 큐 도입
- [ ] 로드 밸런싱
  - 서비스 인스턴스 확장
  - 부하 분산

### 5.4 CI/CD 파이프라인 ⏳
**우선순위**: 중간  
**예상 기간**: 1주

**작업 내용**:
- [ ] 자동 빌드
  - GitHub Actions
  - Docker 이미지 빌드
- [ ] 자동 테스트
  - 단위 테스트
  - 통합 테스트
- [ ] 자동 배포
  - 스테이징 환경
  - 프로덕션 환경
- [ ] 롤백
  - 배포 실패 시 자동 롤백

---

## Phase 6: 머신러닝 전략 (장기)

**목표**: AI 기반 투자 전략

### 6.1 데이터 준비 ⏳
**예상 기간**: 2주

**작업 내용**:
- [ ] 피처 엔지니어링
  - 기술적 지표 조합
  - 재무 비율 조합
  - 시계열 특성 추출
- [ ] 데이터 정규화
  - Min-Max Scaling
  - Z-Score Normalization
- [ ] 학습/검증/테스트 분할
  - 시계열 고려 분할
  - Walk-Forward 검증

### 6.2 모델 개발 ⏳
**예상 기간**: 4주

**작업 내용**:
- [ ] 회귀 모델
  - 수익률 예측
  - Linear Regression, Random Forest
- [ ] 분류 모델
  - 상승/하락 예측
  - Logistic Regression, XGBoost
- [ ] 딥러닝 모델
  - LSTM, GRU
  - Transformer
- [ ] 강화학습
  - DQN, PPO
  - 포트폴리오 최적화

### 6.3 모델 평가 및 배포 ⏳
**예상 기간**: 2주

**작업 내용**:
- [ ] 백테스팅
  - 과거 데이터로 성능 검증
  - 과적합 확인
- [ ] 모델 서빙
  - TensorFlow Serving
  - REST API 제공
- [ ] A/B 테스팅
  - 기존 전략 vs ML 전략
  - 성과 비교
- [ ] 모델 모니터링
  - 예측 정확도 추적
  - 모델 재학습

---

## 우선순위 요약

### 높음 (1-2주 내)
1. Phase 1: 데이터 정합성 검증
2. Phase 2: 리스크 지표 구현
3. Phase 2: 거래 비용 반영

### 중간 (1-2개월 내)
1. Phase 1: 성능 모니터링
2. Phase 2: 전략 관리 개선
3. Phase 3: 멀티팩터 전략
4. Phase 3: 동적 자산배분

### 낮음 (3-6개월 내)
1. Phase 3: 섹터 로테이션
2. Phase 3: 리스크 패리티
3. Phase 4: 실시간 시스템
4. Phase 5: 프로덕션 준비

### 장기 (6개월 이상)
1. Phase 6: 머신러닝 전략

---

## 참고 자료

- [구현 현황](./Implementation_Status.md)
- [검증 가이드](./Testing_and_Verification.md)
- [README](../README.md)
