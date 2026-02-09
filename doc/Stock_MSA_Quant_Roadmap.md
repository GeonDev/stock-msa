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

### 🚧 Phase 2: 백테스팅 엔진 구축 (진행 중)

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

**6. 전략 프레임워크**
- ✅ `Strategy` 인터페이스 정의
  - `getName()`: 전략 이름
  - `rebalance()`: 리밸런싱 로직
- ✅ `EqualWeightStrategy` 구현
  - 유니버스 내 모든 종목 동일 비중 배분
  - 현금 잔고 고려한 매수/매도 주문 생성
- ✅ `StrategyFactory`: 전략 팩토리 패턴

**7. 서비스 간 통신**
- ✅ HTTP Client 구현 (RestClient 기반)
  - `CorpClient`: stock-corp 서비스 연동
  - `PriceClient`: stock-price 서비스 연동
  - `FinanceClient`: stock-finance 서비스 연동
- ✅ 타임아웃 설정 (연결 5초, 읽기 10초)
- ✅ 프로파일별 URL 설정 (local/prod)

**8. REST API 구현**
- ✅ `BacktestController`: 백테스팅 API
  - `POST /api/v1/strategy/backtest`: 백테스팅 시작
  - `GET /api/v1/strategy/backtest/{id}/result`: 결과 조회
  - `GET /api/v1/strategy/backtest/{id}/snapshots`: 스냅샷 조회
- ✅ SpringDoc OpenAPI 문서화

**9. 데이터베이스 마이그레이션**
- ✅ Flyway 기반 스키마 관리
  - `TB_BACKTEST_SIMULATION`
  - `TB_PORTFOLIO_SNAPSHOT`
  - `TB_BACKTEST_RESULT`
  - `TB_TRADE_HISTORY`
- ✅ 인덱스 최적화 (simulation_id, snapshot_date 등)

**10. 설정 및 배포**
- ✅ `application.yaml` 프로파일 설정 (local/prod)
- ✅ Dockerfile 작성
- ✅ Tinylog 로깅 설정
- ✅ Docker Compose 통합
- ✅ Healthcheck 설정

#### 남은 작업

**1. 유니버스 필터링 고도화**
- [ ] 복합 조건 지원 강화
- [ ] 동적 쿼리 생성 (QueryDSL 검토)
- [ ] 필터링 성능 최적화

**2. 추가 전략 구현**
- [ ] **MomentumStrategy** (모멘텀 전략)
  - 과거 N일 수익률 상위 종목 선정
  - 모멘텀 지표 활용 (1개월, 3개월, 6개월)
- [ ] **LowVolatilityStrategy** (저변동성 전략)
  - 변동성 하위 종목 선정
  - 안정적 수익 추구
- [ ] **ValueStrategy** (가치 투자 전략)
  - PER, PBR, ROE 등 가치 지표 기반
  - 재무 데이터 연동 필요
- [ ] **팩터 기반 전략**
  - 멀티 팩터 조합
  - 팩터 스코어링 시스템

**3. 성능 최적화**
- [ ] **가격 데이터 캐싱**
  - Redis 또는 In-Memory 캐시 도입
  - 반복 조회 최소화
- [ ] **배치 조회 최적화**
  - 대량 데이터 조회 시 페이징 처리
  - N+1 쿼리 문제 해결
- [ ] **병렬 처리**
  - 장기 백테스팅 (10년 이상) 병렬화
  - CompletableFuture 활용

**4. 비동기 처리 개선**
- [ ] **@Async 기반 백테스팅 실행**
  - 백테스팅 요청 즉시 응답
  - 백그라운드 실행
- [ ] **진행 상황 추적 API**
  - 실시간 진행률 조회
  - 중간 결과 확인
- [ ] **WebSocket 실시간 알림** 
  - 백테스팅 완료 알림
  - 진행률 실시간 전송

**5. 테스트 및 검증**
- [ ] 단위 테스트 작성
  - 성과 지표 계산 로직
  - 전략별 리밸런싱 로직
- [ ] 통합 테스트
  - 전체 시뮬레이션 플로우
  - 서비스 간 통신
- [ ] 실제 데이터 검증
  - 알려진 전략 재현 (예: 저PBR)
  - 성과 지표 정확도 검증

#### 기술적 고려사항

**데이터 정합성**
- 수정주가(Adjusted Price) 사용 필수
- 휴장일 처리 로직 필수
- 상장폐지 종목 자동 제외

**에러 처리**
- 시뮬레이션 중 오류 발생 시 `FAILED` 상태 처리
- 에러 메시지 로깅 및 사용자 알림
- 재시도 로직 검토

**확장성**
- 전략 추가 시 Strategy 인터페이스 구현만으로 확장 가능
- 팩토리 패턴으로 전략 관리
- 플러그인 아키텍처 검토

#### 예상 완료: 2026-03

---

### 🎯 Phase 3: 포트폴리오 관리 및 리밸런싱 (계획)

**목표**: 실전(또는 모의) 투자 포트폴리오 관리 및 자동 리밸런싱

#### 주요 기능

**1. 실시간 포트폴리오 관리**
- [ ] 현재 보유 종목 및 비중 추적
- [ ] 평단가 자동 계산
- [ ] 실시간 평가손익 계산
- [ ] 포트폴리오 성과 모니터링

**2. 리밸런싱 엔진**
- [ ] 목표 비중 vs 현재 비중 괴리율 계산
- [ ] 매매 주문서 자동 생성
- [ ] 최소 거래 단위 고려
- [ ] 거래 비용 최적화

**3. 리밸런싱 배치**
- [ ] 주기적 실행 (월별/분기별)
- [ ] 임계값 기반 트리거 (괴리율 > 5%)
- [ ] 알림 기능 (이메일/슬랙)

**4. 주문 실행 인터페이스**
- [ ] 증권사 API 연동 준비
- [ ] 모의 투자 모드
- [ ] 주문 이력 관리

#### 기술적 고려사항
- 증권사 API 연동 (키움증권, 한국투자증권 등)
- 실시간 시세 수신 (WebSocket)
- 주문 체결 확인 및 재시도 로직
- 거래 가능 시간 체크

#### 예상 완료: 2026-06

---

### 🔬 Phase 4: 전략 발굴 및 최적화 (계획)

**목표**: 데이터 기반 알파(Alpha) 발굴 및 전략 최적화

#### 주요 기능

**1. 팩터 분석 도구**
- [ ] 단일 팩터 유효성 검증
- [ ] 팩터 간 상관관계 분석
- [ ] 팩터 조합 최적화

**2. 조건검색(Screening) API**
- [ ] 복합 조건 기반 종목 추출
- [ ] 예: `PER < 10 AND PBR < 1 AND ROE > 15`
- [ ] 동적 쿼리 생성 (QueryDSL)

**3. 전략 최적화 엔진**
- [ ] 파라미터 그리드 서치
- [ ] 유전 알고리즘 기반 최적화
- [ ] Walk-forward 분석

**4. 머신러닝 통합**
- [ ] 종목 선정 모델 (Classification)
- [ ] 수익률 예측 모델 (Regression)
- [ ] 이상 탐지 (Anomaly Detection)

#### 기술 스택 검토
- Python 연동 (Py4J 또는 REST API)
- Scikit-learn, XGBoost, LightGBM
- Feature Engineering 파이프라인

#### 예상 완료: 2026-09

---

### 📊 Phase 5: 시각화 및 대시보드 (계획)

**목표**: 직관적인 데이터 시각화 및 모니터링 대시보드

#### 주요 기능

**1. 백테스팅 결과 시각화**
- [ ] 누적 수익률 그래프
- [ ] 낙폭(Drawdown) 차트
- [ ] 월별/연도별 수익률 히트맵
- [ ] 종목별 기여도 분석

**2. 포트폴리오 대시보드**
- [ ] 실시간 자산 현황
- [ ] 섹터별 비중 파이 차트
- [ ] 손익 추이 그래프
- [ ] 리밸런싱 알림

**3. 전략 비교 도구**
- [ ] 여러 전략 성과 비교
- [ ] 리스크-수익률 산점도
- [ ] 상관관계 매트릭스

#### 기술 스택 검토
- **Frontend**: React + TypeScript
- **Chart Library**: Recharts, D3.js, or Apache ECharts
- **Dashboard**: Grafana (시계열 데이터)
- **Real-time**: WebSocket + Server-Sent Events

#### 예상 완료: 2026-12

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
1. **Phase 2 백테스팅 엔진 완성**
   - 추가 전략 구현 (Momentum, Value)
   - 유니버스 필터링 고도화
   - 성능 최적화 (캐싱, 병렬 처리)

2. **테스트 및 검증**
   - 실제 과거 데이터로 백테스팅 실행
   - 알려진 전략(예: 저PBR) 재현 검증
   - 성과 지표 정확도 검증

### 중기 (3개월)
1. **Phase 3 포트폴리오 관리 시작**
   - 데이터베이스 스키마 설계
   - 리밸런싱 로직 구현
   - 모의 투자 모드 개발

2. **모니터링 시스템 구축**
   - Prometheus + Grafana 설정
   - 핵심 메트릭 정의 및 수집
   - 알림 규칙 설정

### 장기 (6개월)
1. Phase 4 전략 발굴 도구 개발
2. Phase 5 시각화 대시보드 구축
3. 실전 투자 준비 (증권사 API 연동)

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

**마지막 업데이트**: 2026-02-06  
**프로젝트 상태**: Phase 2 진행 중 (백테스팅 엔진 구축 완료, 추가 전략 및 최적화 작업 진행 예정)
