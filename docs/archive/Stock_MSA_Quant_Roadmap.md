# Stock-MSA 퀀트 투자 시스템 로드맵

## 📋 프로젝트 개요

**Stock-MSA**는 공공데이터포털 및 DART API를 활용하여 **데이터 수집부터 전략 검증, 포트폴리오 관리까지 자동화된 동적 자산배분 시스템**을 구축하는 MSA 기반 퀀트 투자 플랫폼입니다.

### 핵심 목표
- ✅ 신뢰할 수 있는 데이터 파이프라인 구축
- ✅ 과학적 백테스팅을 통한 전략 검증
- 🎯 실전 투자 자동화 및 리밸런싱
- 🎯 지속적인 알파(Alpha) 발굴

---

## 🏗️ 현재 시스템 아키텍처 (2026-02 기준)

```
┌─────────────────────────────────────────────────────────────┐
│                    stock-gateway (8080)                      │
│              API Gateway + Security (WebFlux)                │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┬─────────┐
        │                     │                     │         │
┌───────▼────────┐   ┌───────▼────────┐   ┌───────▼────────┐ │
│  stock-corp    │   │ stock-finance  │   │  stock-price   │ │
│    (8081)      │   │    (8082)      │   │    (8083)      │ │
│ 기업정보/업종   │   │  재무제표/검증  │   │ 주가/기술지표   │ │
└────────────────┘   └────────────────┘   └────────────────┘ │
        │                     │                     │         │
┌───────▼────────┐   ┌───────▼────────┐   ┌───────▼────────┐ │
│ stock_corp_db  │   │stock_finance_db│   │ stock_price_db │ │
│    (3306)      │   │    (3307)      │   │    (3308)      │ │
└────────────────┘   └────────────────┘   └────────────────┘ │
                                                               │
                                                    ┌──────────▼────────┐
                                                    │  stock-strategy   │
                                                    │      (8084)       │
                                                    │  백테스팅 엔진     │
                                                    └───────────────────┘
                                                               │
                                                    ┌──────────▼────────┐
                                                    │stock_strategy_db  │
                                                    │     (3310)        │
                                                    └───────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              stock-discovery (8761) - Eureka                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│         stock_batch_db (3309) - Shared Batch Metadata        │
└─────────────────────────────────────────────────────────────┘
```

### 기술 스택
- **Backend**: Spring Boot 3.4.8, Java 21
- **Cloud**: Spring Cloud 2024.0.2 (Eureka, Gateway)
- **Database**: MySQL 8.0 (서비스별 독립 DB)
- **Batch**: Spring Batch (Chunk-oriented)
- **Container**: Docker Compose with Healthcheck
- **Precision**: BigDecimal (DECIMAL(25, 4)) for all financial data
- **Technical Analysis**: Ta4j Library

---

## 📊 Phase 별 상세 로드맵

### 전략적 방향성

본 프로젝트는 **"좋은 전략 발굴 → 종목 추천 → 포트폴리오 관리 → 자동 매매"** 순서로 진행됩니다.

**핵심 철학**:
1. **전략 검증이 최우선**: 백테스팅을 통해 과학적으로 검증된 전략만 사용
2. **종목 추천 시스템**: 검증된 전략으로 매일 투자 아이디어 제공
3. **포트폴리오 관리**: 추천 종목 기반 포트폴리오 구성 및 리밸런싱
4. **자동 매매는 최종 단계**: 충분한 검증 후 선택적으로 적용

---

### ✅ Phase 0: 인프라 구축 (완료)

**목표**: MSA 기반 인프라 및 기본 데이터 수집 파이프라인 구축

#### 완료된 작업
- ✅ Gradle Multi-Module Monorepo 구조 확립
- ✅ Service Discovery (Eureka) 구축
- ✅ API Gateway (Spring Cloud Gateway) with Reactive Security
- ✅ 도메인별 마이크로서비스 분리 (corp, finance, price)
- ✅ Docker Compose 기반 컨테이너화
- ✅ 데이터베이스 독립성 확보 (서비스별 DB + 공유 Batch DB)
- ✅ 외부 API 연동 (공공데이터포털, DART)
- ✅ Flyway 기반 스키마 버전 관리
- ✅ 배치 작업 API 트리거 방식 구현

#### 주요 성과
- Apple Silicon(M1/M2) 및 x86 환경 모두 지원
- Healthcheck 기반 서비스 의존성 관리
- Docker Volume을 통한 데이터 영속성 보장

---

### ✅ Phase 1: 데이터 무결성 및 전처리 (완료)

**목표**: 퀀트 분석을 위한 고품질 데이터 확보 및 전처리

#### 완료된 작업

**1. BigDecimal 정밀도 전환**
- 모든 금융 데이터(가격, 거래량, 비율, 지표)를 BigDecimal로 전환
- DECIMAL(25, 4) 데이터베이스 타입 적용
- 오버플로우 및 부동소수점 오차 원천 차단

**2. 수정주가(Adjusted Price) 계산 엔진**
- 증자, 감자, 액면분할 등 기업 이벤트 수집 (`CorpEventHistory`)
- 과거 주가 자동 보정 로직 구현 (`AdjustedPriceService`)
- 수정종가(`adjClosePrice`) 사전 계산 및 저장

**3. 기술적 지표 계산 모듈**
- Ta4j 라이브러리 통합
- 이동평균선(MA), RSI, MACD, Bollinger Bands 계산
- **모멘텀 지표** (1개월, 3개월, 6개월) 추가
- 모든 지표를 BigDecimal 정밀도로 사전 계산

**4. 데이터 정합성 검증**
- 재무제표 대차대조표 등식 검증 (자산 = 부채 + 자본)
- 필수 필드 존재 여부 확인
- ValidationStatus enum을 통한 데이터 품질 추적
- 기술적 지표 계산 시 최소 300거래일 데이터 확보 검증

**5. Chunk-based Batch 표준화**
- Reader-Processor-Writer 패턴 적용
- ApplicationConstants를 통한 청크 크기 중앙 관리
- 대용량 데이터 처리 안정성 확보

#### 주요 성과
- 백테스팅에 필요한 모든 데이터 품질 보장
- 계산 정밀도 향상으로 신뢰도 높은 분석 기반 마련
- 배치 처리 성능 최적화

---

### ✅ Phase 2: 백테스팅 엔진 구축 (완료)

**목표**: 과거 데이터 기반 전략 유효성 검증 시스템 구축

#### 완료된 작업

**1. stock-strategy 서비스 생성 및 인프라 구축**
- ✅ 독립 마이크로서비스 생성 (Port: 8084)
- ✅ 독립 데이터베이스 구성 (stock_strategy_db, Port: 3310)
- ✅ Docker Compose 통합 완료
- ✅ Gateway 라우팅 설정 (`/api/v1/strategy/**`)
- ✅ Eureka 서비스 등록
- ✅ 멀티 데이터소스 설정 (Main DB + Batch DB)

**2. 핵심 엔티티 및 데이터 모델 설계**
- ✅ `BacktestSimulation`: 시뮬레이션 메타데이터
  - 전략명, 기간, 초기자본, 리밸런싱 주기, 수수료/세금, 상태 관리
- ✅ `PortfolioSnapshot`: 포트폴리오 스냅샷
  - 일별 자산 가치, 현금 잔고, 보유 종목 (JSON 저장)
- ✅ `BacktestResult`: 성과 지표
  - CAGR, MDD, Sharpe Ratio, Volatility, Win Rate 등
- ✅ `TradeHistory`: 매매 이력
  - 거래일, 종목, 주문 타입, 수량, 가격, 수수료, 세금
- ✅ Enum 타입 정의
  - `SimulationStatus`: PENDING, RUNNING, COMPLETED, FAILED
  - `RebalancingPeriod`: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
  - `OrderType`: BUY, SELL

**3. 시뮬레이션 엔진 구현**
- ✅ `SimulationEngine`: 핵심 백테스팅 로직
  - 일별 시뮬레이션 루프
  - 휴장일 처리 (DayOffService 활용)
  - 리밸런싱 주기 관리
  - 매매 수수료 및 세금 계산
  - 포트폴리오 상태 추적 및 업데이트
  - 스냅샷 저장 (리밸런싱일 기준)
  - 전략별 설정 전달 (ValueStrategyConfig 등)

**4. 성과 분석 모듈**
- ✅ `PerformanceCalculationService`: 성과 지표 계산
  - **CAGR** (연평균 성장률): `((최종가치 / 초기자본) ^ (1 / 연수)) - 1`
  - **MDD** (최대 낙폭): 기간 중 최대 손실률
  - **Sharpe Ratio** (샤프 지수): 위험 대비 수익률
  - **Volatility** (변동성): 일별 수익률 표준편차 (연환산)
  - **Win Rate** (승률): 수익 거래 비율
  - BigDecimal 정밀도 계산

**5. 유니버스 필터링**
- ✅ `UniverseFilterService`: 종목 선정 로직
  - 시장별 필터링 (KOSPI, KOSDAQ)
  - 시가총액 기반 필터링
  - 거래량 기반 필터링
  - 업종 제외 필터링
  - `UniverseFilterCriteria` DTO 지원

**6. 전략 프레임워크 및 구현**
- ✅ `Strategy` 인터페이스 정의
  - `getName()`: 전략 이름
  - `rebalance()`: 리밸런싱 로직
- ✅ **EqualWeightStrategy** (동일 비중 전략)
  - 유니버스 내 모든 종목 동일 비중 배분
  - 현금 잔고 고려한 매수/매도 주문 생성
- ✅ **MomentumStrategy** (모멘텀 전략)
  - 과거 N일 수익률 상위 종목 선정
  - 모멘텀 지표 활용 (1개월, 3개월, 6개월)
  - 상위 20개 종목 동일 비중 배분
- ✅ **LowVolatilityStrategy** (저변동성 전략)
  - 최근 60일 변동성 계산 (일별 수익률 표준편차)
  - 변동성 하위 20개 종목 선정
  - 안정적 수익 추구
- ✅ **ValueStrategy** (가치 투자 전략)
  - PER, PBR, ROE 기반 가치 스코어 계산
  - 가중치 커스터마이징 지원 (기본: PER 30%, PBR 30%, ROE 40%)
  - 상위 N개 종목 선정 (기본: 20개)
  - 재무 지표 배치 조회 최적화
- ✅ `StrategyFactory`: 전략 팩토리 패턴

**7. 서비스 간 통신**
- ✅ HTTP Client 구현 (RestClient 기반)
  - `CorpClient`: stock-corp 서비스 연동
  - `PriceClient`: stock-price 서비스 연동
  - `FinanceClient`: stock-finance 서비스 연동 (신규)
- ✅ 타임아웃 설정 (연결 5초, 읽기 10초)
- ✅ 프로파일별 URL 설정 (local/prod)
- ✅ 배치 조회 API 구현 (N+1 문제 방지)

**8. stock-finance 내부 API 추가**
- ✅ `InternalFinanceController`: 재무 지표 조회 API
  - `/internal/indicators/batch`: 여러 종목의 특정 날짜 재무 지표 배치 조회
  - `/internal/indicators/{corpCode}/latest`: 최신 재무 지표 조회
- ✅ `CorpFinanceIndicatorMapper`: 엔티티-DTO 변환 (MapStruct)
- ✅ `CorpFinanceIndicatorDto`: 재무 지표 데이터 전송 객체

**9. 전략 설정 DTO**
- ✅ `ValueStrategyConfig`: 가치 투자 전략 설정
  - 선정 종목 수 (topN)
  - PER, PBR, ROE 가중치 설정
  - 가중치 합계 검증 로직
- ✅ `BacktestRequest`에 전략별 설정 필드 추가
  - `valueStrategyConfig`: VALUE 전략 사용 시 커스텀 설정

**10. 입력값 검증**
- ✅ Spring Validation 적용
  - `@Valid` + `@RequestBody`: DTO 검증
  - `@Validated` + 제약 어노테이션: RequestParam 검증
- ✅ 글로벌 예외 처리
  - `GlobalExceptionHandler`: 일관된 에러 응답
  - `ErrorResponse`: 표준화된 에러 DTO
  - `MethodArgumentNotValidException`, `ConstraintViolationException` 처리

**11. REST API 구현**
- ✅ `BacktestController`: 백테스팅 API
  - `POST /api/v1/strategy/backtest`: 백테스팅 시작
  - `GET /api/v1/strategy/backtest/{id}/result`: 결과 조회
  - `GET /api/v1/strategy/backtest/{id}/snapshots`: 스냅샷 조회
- ✅ SpringDoc OpenAPI 문서화
- ✅ 입력값 검증 적용

**12. 데이터베이스 마이그레이션**
- ✅ Flyway 기반 스키마 관리
  - `TB_BACKTEST_SIMULATION`
  - `TB_PORTFOLIO_SNAPSHOT`
  - `TB_BACKTEST_RESULT`
  - `TB_TRADE_HISTORY`
- ✅ 인덱스 최적화 (simulation_id, snapshot_date 등)

**13. 설정 및 배포**
- ✅ `application.yaml` 프로파일 설정 (local/prod)
- ✅ Dockerfile 작성
- ✅ Tinylog 로깅 설정
- ✅ Docker Compose 통합
- ✅ Healthcheck 설정

#### 구현된 전략 상세

**1. EqualWeightStrategy (동일 비중)**
- 유니버스 내 모든 종목에 동일 비중 배분
- 가장 단순하지만 효과적인 분산 투자 전략
- 리밸런싱을 통한 자동 수익 실현

**2. MomentumStrategy (모멘텀)**
- 과거 수익률 상위 종목 선정 (기본: 상위 20개)
- 모멘텀 지표 활용 (1개월, 3개월, 6개월)
- 추세 추종 전략

**3. LowVolatilityStrategy (저변동성)**
- 최근 60일 변동성 계산
- 변동성 하위 20개 종목 선정
- 저변동성 이상 현상(Low Volatility Anomaly) 활용
- 방어적 투자 전략

**4. ValueStrategy (가치 투자)**
- PER, PBR, ROE 기반 가치 스코어 계산
- 가중치 커스터마이징 지원
  - 기본값: PER 30%, PBR 30%, ROE 40%
  - 보수적: PER 50%, PBR 40%, ROE 10%
  - 성장 중시: PER 20%, PBR 20%, ROE 60%
- 저평가 우량주 발굴
- 벤저민 그레이엄, 워렌 버핏의 가치 투자 철학 계량화

#### 주요 성과
- **4가지 핵심 전략 완성**: Equal Weight, Momentum, Low Volatility, Value
- **전략별 커스터마이징 지원**: 가중치, 종목 수 등 유연한 설정
- **배치 조회 최적화**: N+1 문제 방지, 성능 향상
- **입력값 검증 강화**: 안정적인 API 운영
- **재무 지표 연동**: stock-finance 서비스와 효율적인 통신

#### 기술적 고려사항

**데이터 정합성**
- ✅ 수정주가(Adjusted Price) 사용
- ✅ 휴장일 처리 로직
- ✅ 상장폐지 종목 자동 제외
- ✅ Stock code 형식 통일 (재무 데이터: 숫자만)

**에러 처리**
- ✅ 시뮬레이션 중 오류 발생 시 `FAILED` 상태 처리
- ✅ 에러 메시지 로깅 및 사용자 알림
- ✅ 재무 지표 누락 시 종목 제외 처리
- ✅ API 호출 실패 시 빈 리스트 반환

**확장성**
- ✅ 전략 추가 시 Strategy 인터페이스 구현만으로 확장 가능
- ✅ 팩토리 패턴으로 전략 관리
- ✅ 전략별 설정 DTO 지원

**성능 최적화**
- ✅ 배치 조회 API 사용 (N+1 문제 방지)
- ✅ BigDecimal 정밀도 계산
- ✅ 효율적인 데이터 구조 활용

#### 완료: 2026-02-15

---

### 🎯 Phase 3: 전략 발굴 및 종목 추천 시스템 (계획)

**목표**: 검증된 전략으로 매일 투자 아이디어 제공 및 전략 최적화

#### 주요 기능

**1. 일일 종목 추천 시스템**
- [ ] **전략별 추천 엔진**
  - 각 전략(Value, Momentum, Low Volatility)별 상위 종목 추출
  - 복합 전략 (멀티 팩터) 추천
  - 추천 이유 및 근거 제공
- [ ] **추천 API**
  - `GET /api/v1/recommendations/daily`: 오늘의 추천 종목
  - `GET /api/v1/recommendations/strategy/{strategyType}`: 전략별 추천
  - `GET /api/v1/recommendations/stock/{stockCode}`: 종목별 분석
- [ ] **추천 이력 관리**
  - 과거 추천 종목 추적
  - 추천 성과 분석
  - 전략별 적중률 계산

**2. 조건검색(Screening) 도구**
- [ ] **복합 조건 검색**
  - 예: `PER < 10 AND PBR < 1 AND ROE > 15`
  - 예: `모멘텀 상위 20% AND 변동성 하위 30%`
- [ ] **동적 쿼리 생성**
  - QueryDSL 기반 유연한 조건 조합
  - 사용자 정의 스크리닝 저장
- [ ] **스크리닝 API**
  - `POST /api/v1/screening/search`: 조건 검색
  - `GET /api/v1/screening/presets`: 사전 정의된 스크리닝
  - `POST /api/v1/screening/custom`: 커스텀 스크리닝 저장

**3. 팩터 분석 도구**
- [ ] **단일 팩터 유효성 검증**
  - 팩터별 수익률 분석
  - 팩터 분위수(Quintile) 성과 비교
  - 시계열 안정성 검증
- [ ] **팩터 간 상관관계 분석**
  - 상관계수 매트릭스
  - 다중공선성 검증
  - 독립적 팩터 발굴
- [ ] **팩터 조합 최적화**
  - 가중치 최적화 (Grid Search)
  - 팩터 스코어링 시스템
  - 백테스팅 기반 검증

**4. 전략 최적화 엔진**
- [ ] **파라미터 최적화**
  - 그리드 서치 (Grid Search)
  - 랜덤 서치 (Random Search)
  - 베이지안 최적화 (Bayesian Optimization)
- [ ] **Walk-forward 분석**
  - 학습 기간 / 검증 기간 분리
  - 과최적화(Overfitting) 방지
  - 실전 적용 가능성 검증
- [ ] **전략 비교 도구**
  - 여러 전략 성과 비교
  - 리스크-수익률 산점도
  - 상관관계 분석

**5. 알림 및 리포트**
- [ ] **일일 추천 알림**
  - 이메일 / Slack / Telegram 연동
  - 매일 장 시작 전 추천 종목 발송
  - 주요 이벤트 알림 (급등/급락, 재무제표 발표 등)
- [ ] **주간/월간 리포트**
  - 전략별 성과 요약
  - 추천 종목 적중률
  - 시장 동향 분석

#### 기술 스택 검토
- **조건 검색**: QueryDSL (동적 쿼리)
- **최적화**: Apache Commons Math, Optuna (Python 연동)
- **알림**: Spring Mail, Slack API, Telegram Bot API
- **스케줄링**: Spring Scheduler, Quartz

#### 예상 완료: 2026-06

---

### 📊 Phase 4: 포트폴리오 관리 및 시뮬레이션 (계획)

**목표**: 추천 종목 기반 포트폴리오 구성 및 모의 투자

#### 주요 기능

**1. 포트폴리오 구성 도구**
- [ ] **추천 기반 포트폴리오 생성**
  - 전략별 추천 종목으로 포트폴리오 자동 구성
  - 목표 비중 설정 (동일 비중 / 가중 비중)
  - 리스크 관리 (최대 종목 비중, 섹터 제한)
- [ ] **포트폴리오 최적화**
  - 평균-분산 최적화 (Mean-Variance Optimization)
  - 리스크 패리티 (Risk Parity)
  - 블랙-리터만 모델 (Black-Litterman)

**2. 모의 투자 (Paper Trading)**
- [ ] **가상 계좌 관리**
  - 초기 자본 설정
  - 실시간 평가손익 계산
  - 거래 이력 추적
- [ ] **자동 리밸런싱**
  - 주기적 리밸런싱 (월별/분기별)
  - 임계값 기반 리밸런싱 (괴리율 > 5%)
  - 리밸런싱 시뮬레이션
- [ ] **성과 모니터링**
  - 실시간 포트폴리오 가치
  - 벤치마크 대비 성과
  - 리스크 지표 (MDD, Sharpe Ratio)

**3. 리밸런싱 엔진**
- [ ] **목표 비중 vs 현재 비중 분석**
  - 괴리율 계산
  - 매매 필요 종목 식별
- [ ] **매매 주문서 생성**
  - 최소 거래 단위 고려
  - 거래 비용 최적화
  - 세금 효율적 매매 (Tax-Loss Harvesting)
- [ ] **실행 계획**
  - 분할 매수/매도 전략
  - 시장 충격 최소화
  - 유동성 고려

**4. 포트폴리오 분석**
- [ ] **리스크 분석**
  - VaR (Value at Risk)
  - CVaR (Conditional VaR)
  - 베타, 알파 계산
- [ ] **기여도 분석**
  - 종목별 수익 기여도
  - 섹터별 비중 및 성과
  - 팩터 익스포저 분석

#### 기술적 고려사항
- 실시간 시세 수신 (WebSocket)
- 포트폴리오 최적화 라이브러리 (PyPortfolioOpt, CVXPY)
- 데이터 시각화 (Chart.js, D3.js)

#### 예상 완료: 2026-09

---

### 🤖 Phase 5: 머신러닝 기반 전략 고도화 (계획)

**목표**: AI/ML을 활용한 고급 전략 개발 및 예측 모델 구축

#### 주요 기능

**1. 종목 선정 모델**
- [ ] **분류 모델 (Classification)**
  - 상승/하락 예측
  - 아웃퍼폼/언더퍼폼 분류
  - 특징 중요도 분석
- [ ] **알고리즘**
  - Random Forest, XGBoost, LightGBM
  - Neural Networks (LSTM, Transformer)
  - Ensemble Methods

**2. 수익률 예측 모델**
- [ ] **회귀 모델 (Regression)**
  - 미래 수익률 예측
  - 변동성 예측
  - 신뢰 구간 제공
- [ ] **시계열 분석**
  - ARIMA, GARCH
  - Prophet (Facebook)
  - 딥러닝 기반 시계열 모델

**3. 이상 탐지 (Anomaly Detection)**
- [ ] **비정상 패턴 감지**
  - 급등/급락 사전 감지
  - 재무제표 이상 징후
  - 거래량 이상 패턴
- [ ] **리스크 조기 경보**
  - 포트폴리오 리스크 증가 감지
  - 시장 위기 신호 포착

**4. Feature Engineering**
- [ ] **자동 특징 생성**
  - 기술적 지표 조합
  - 재무 비율 파생 변수
  - 시장 센티먼트 지표
- [ ] **특징 선택**
  - 상관관계 기반 필터링
  - 재귀적 특징 제거 (RFE)
  - SHAP 값 기반 중요도

**5. 모델 관리**
- [ ] **MLOps 파이프라인**
  - 모델 학습 자동화
  - 버전 관리 (MLflow)
  - A/B 테스팅
- [ ] **성능 모니터링**
  - 예측 정확도 추적
  - 모델 드리프트 감지
  - 재학습 트리거

#### 기술 스택
- **Python 연동**: Py4J, REST API, gRPC
- **ML 라이브러리**: Scikit-learn, XGBoost, LightGBM, TensorFlow, PyTorch
- **MLOps**: MLflow, Kubeflow, DVC
- **Feature Store**: Feast, Tecton

#### 예상 완료: 2026-12

---

### 🎨 Phase 6: 시각화 및 대시보드 (계획)

**목표**: 직관적인 데이터 시각화 및 모니터링 대시보드

#### 주요 기능

**1. 종목 추천 대시보드**
- [ ] **오늘의 추천 종목**
  - 전략별 추천 카드
  - 추천 이유 및 근거
  - 과거 추천 성과
- [ ] **종목 상세 분석**
  - 가격 차트 (캔들스틱, 이동평균선)
  - 재무 지표 시각화
  - 뉴스 및 공시 정보

**2. 백테스팅 결과 시각화**
- [ ] **성과 차트**
  - 누적 수익률 그래프
  - 낙폭(Drawdown) 차트
  - 월별/연도별 수익률 히트맵
- [ ] **비교 분석**
  - 여러 전략 성과 비교
  - 벤치마크 대비 성과
  - 리스크-수익률 산점도

**3. 포트폴리오 대시보드**
- [ ] **실시간 자산 현황**
  - 총 자산 가치
  - 현금 잔고
  - 보유 종목 목록
- [ ] **시각화**
  - 섹터별 비중 파이 차트
  - 손익 추이 그래프
  - 종목별 기여도 차트

**4. 전략 분석 도구**
- [ ] **팩터 분석 시각화**
  - 팩터 수익률 차트
  - 상관관계 히트맵
  - 분위수 성과 비교
- [ ] **최적화 결과**
  - 효율적 투자선 (Efficient Frontier)
  - 파라미터 민감도 분석
  - Walk-forward 결과

**5. 알림 센터**
- [ ] **실시간 알림**
  - 추천 종목 업데이트
  - 리밸런싱 알림
  - 중요 이벤트 알림
- [ ] **알림 설정**
  - 알림 채널 선택 (이메일, Slack, Telegram)
  - 알림 조건 커스터마이징
  - 알림 이력 조회

#### 기술 스택
- **Frontend**: React + TypeScript, Next.js
- **Chart Library**: Recharts, D3.js, Apache ECharts, TradingView Lightweight Charts
- **Dashboard**: Grafana (시계열 데이터)
- **Real-time**: WebSocket, Server-Sent Events
- **UI Framework**: Material-UI, Ant Design, Tailwind CSS

#### 예상 완료: 2027-03

---

### 🔗 Phase 7: 자동 매매 시스템 (최종 단계)

**목표**: 검증된 전략의 자동 실행 (선택적 기능)

#### 주요 기능

**1. 증권사 API 연동**
- [ ] **주문 실행**
  - 시장가/지정가 주문
  - 조건부 주문
  - 주문 취소/정정
- [ ] **계좌 조회**
  - 잔고 조회
  - 체결 내역
  - 미체결 주문
- [ ] **실시간 시세**
  - WebSocket 기반 실시간 호가
  - 체결가 수신
  - 시장 상태 모니터링

**2. 자동 매매 엔진**
- [ ] **전략 실행**
  - 추천 종목 자동 매수
  - 리밸런싱 자동 실행
  - 손절/익절 자동화
- [ ] **리스크 관리**
  - 최대 손실 제한
  - 포지션 크기 제어
  - 긴급 청산 기능
- [ ] **실행 최적화**
  - TWAP (Time-Weighted Average Price)
  - VWAP (Volume-Weighted Average Price)
  - 스마트 주문 라우팅

**3. 모니터링 및 제어**
- [ ] **실시간 모니터링**
  - 주문 상태 추적
  - 체결 확인
  - 슬리피지 분석
- [ ] **수동 개입**
  - 긴급 정지 버튼
  - 수동 주문 실행
  - 전략 일시 중지
- [ ] **감사 로그**
  - 모든 주문 기록
  - 의사결정 근거 저장
  - 규정 준수 보고서

**4. 안전 장치**
- [ ] **사전 검증**
  - 주문 전 잔고 확인
  - 거래 가능 시간 체크
  - 일일 거래 한도 설정
- [ ] **에러 처리**
  - 주문 실패 시 재시도
  - 네트워크 오류 대응
  - 비상 연락 시스템
- [ ] **백업 시스템**
  - 주문 데이터 백업
  - 장애 복구 계획
  - 수동 전환 프로세스

#### 기술적 고려사항
- **증권사 API**: 키움증권 OpenAPI, 한국투자증권 API, eBest API
- **보안**: API 키 암호화, 2FA 인증, IP 화이트리스트
- **안정성**: Circuit Breaker, Retry 로직, Fallback 메커니즘
- **규정 준수**: 금융위원회 규정, 거래소 규칙, 세법

#### ⚠️ 중요 고려사항
- 자동 매매는 **충분한 검증 후** 선택적으로 적용
- 초기에는 **소액으로 테스트** 운영
- **리스크 관리**가 최우선
- 법적 책임 및 규정 준수 필수

#### 예상 완료: 2027-06 (선택적)

---

## 💡 추천 퀀트 투자 전략

현재 시스템이 수집하는 **일별 종가(End-of-Day) 데이터**에 최적화된 전략들입니다.

### A. 동적 자산 배분 (Dynamic Asset Allocation)
개별 종목의 등락보다 **시장 전체의 국면(Market Regime)**을 판단하여 주식, 채권, 현금 비중을 조절하는 전략입니다.

**핵심 로직**
- **상대 모멘텀**: 최근 1~12개월 수익률이 우수한 자산군 매수
- **절대 모멘텀**: 하락 추세(예: 이동평균선 하회) 시 현금화하여 방어

**장점**: 데이터 갱신 주기가 '일' 단위인 현재 시스템에서 **월 1회 리밸런싱**으로 운용하기 최적

**대표 전략**: VAA(Vigilant Asset Allocation), LAA(Lethargic Asset Allocation)

### B. 팩터 투자 (Factor Investing)
수치화된 지표(Factor)를 기준으로 전 종목의 순위를 매겨 포트폴리오를 구성합니다.

**활용 가능 팩터**
- **가격 모멘텀**: 52주 신고가 근접 종목, 6개월 상대 수익률 상위
- **저변동성 (Low Volatility)**: 최근 1년 일간 변동성이 낮은 우상향 종목
- **소형주 효과**: 자본금/시가총액 데이터를 활용한 하위 20% 종목군

**운용 방식**: 매일 밤 Batch로 전 종목 Score 계산 및 랭킹 저장 → 상위 20개 종목 월 단위 교체

### C. 계절성 및 캘린더 효과 (Seasonality)
특정 시기에 반복되는 통계적 패턴을 활용합니다.

**전략 예시**
- **월말월초 효과**: 월말 수급 유입을 겨냥한 월말 매수 ~ 월초 매도
- **할로윈 효과**: 11월 매수 ~ 4월 매도 (겨울~봄 수익률 우위 활용)

**장점**: 복잡한 연산 없이 날짜(`Date`) 데이터만으로 구현 가능

### D. 시스템 적용 아키텍처
1. **Data Processing (16:00~)**: 장 마감 후 공공데이터 API로 당일 OHLCV 수집 및 적재
2. **Signal Generation (Batch)**: 수집 완료 후 전략 로직(모멘텀, 랭킹 등) 수행 → **'익일 매매 추천 리스트'** 생성 및 DB 저장
3. **Action (Next Day 08:30)**: 사용자 알림(Slack/Telegram) 전송 또는 증권사 API 연동 예약 주문

---

## 🔧 기술적 개선 과제

### 성능 최적화
- [ ] Redis 캐싱 도입 (가격 데이터, 기술적 지표)
- [ ] 데이터베이스 인덱스 최적화
- [ ] 배치 처리 병렬화 (Parallel Stream, CompletableFuture)
- [ ] 읽기 전용 Replica DB 구성

### 모니터링 및 관찰성
- [ ] Prometheus + Grafana 메트릭 수집
- [ ] 분산 추적 (Spring Cloud Sleuth + Zipkin)
- [ ] 중앙 집중식 로깅 (ELK Stack)
- [ ] 알림 시스템 (Slack, Email)

### 보안 강화
- [ ] JWT 기반 인증/인가
- [ ] API Rate Limiting
- [ ] 민감 데이터 암호화 (Jasypt)
- [ ] 감사 로그 (Audit Trail)

### 테스트 자동화
- [ ] 단위 테스트 커버리지 80% 이상
- [ ] 통합 테스트 (Testcontainers)
- [ ] 성능 테스트 (JMeter, Gatling)
- [ ] CI/CD 파이프라인 (GitHub Actions)

---

## 📈 성공 지표 (KPI)

### 데이터 품질
- ✅ 데이터 정합성 검증 통과율: 95% 이상
- ✅ 기술적 지표 계산 정확도: 100%
- 🎯 데이터 수집 지연 시간: 1시간 이내

### 백테스팅 성능
- 🎯 10년 백테스팅 실행 시간: 5분 이내
- 🎯 100개 종목 시뮬레이션: 10분 이내
- 🎯 동시 백테스팅 처리: 10개 이상

### 시스템 안정성
- 🎯 서비스 가용성: 99.9% 이상
- 🎯 배치 작업 성공률: 99% 이상
- 🎯 평균 응답 시간: 500ms 이하

---

## 🚀 다음 단계 (Immediate Next Steps)

### 단기 (1개월)
1. **Phase 3 전략 발굴 및 종목 추천 시작**
   - 일일 종목 추천 API 설계
   - 조건검색(Screening) 기능 구현
   - 추천 이력 관리 시스템

2. **백테스팅 엔진 고도화**
   - 성능 최적화 (캐싱, 병렬 처리)
   - 비동기 처리 개선
   - 실제 데이터 검증

### 중기 (3개월)
1. **종목 추천 시스템 완성**
   - 전략별 추천 엔진 구현
   - 알림 시스템 구축 (이메일, Slack)
   - 추천 성과 분석 도구

2. **팩터 분석 도구 개발**
   - 단일 팩터 유효성 검증
   - 팩터 조합 최적화
   - 백테스팅 기반 검증

### 장기 (6개월)
1. **Phase 4 포트폴리오 관리 시작**
   - 모의 투자 시스템 구축
   - 포트폴리오 최적화 도구
   - 리밸런싱 시뮬레이션

2. **머신러닝 기반 전략 연구**
   - 종목 선정 모델 개발
   - 수익률 예측 모델
   - Feature Engineering 파이프라인

---

## 📚 참고 자료

### 외부 API
- [공공데이터포털](https://www.data.go.kr/) - 주가, 재무 데이터
- [DART](https://opendart.fss.or.kr/) - 공시 정보

### 기술 문서
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Ta4j Documentation](https://ta4j.github.io/ta4j-wiki/)

### 퀀트 투자 이론
- "Quantitative Momentum" by Wesley Gray
- "Factor Investing" by Andrew Ang
- "Advances in Financial Machine Learning" by Marcos López de Prado

---

**마지막 업데이트**: 2026-02-15  
**프로젝트 상태**: Phase 2 완료 (백테스팅 엔진 구축 완료, 4가지 핵심 전략 구현 완료)
