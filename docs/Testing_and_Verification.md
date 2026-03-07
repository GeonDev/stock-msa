# AI Agent 검증 가이드 (Ralph Loop PRD)

<!-- AI Agent Instructions (Ralph Loop) -->
> **🤖 AI 에이전트 행동 지침 (Ralph Loop)**
> 1. 이전 대화나 컨텍스트는 무시하고, 이 문서의 `[ ]` (미완료) 상태인 **최상위 작업 하나만** 선택하세요.
> 2. 해당 작업을 완수하기 위한 코드를 분석, 작성 또는 수정하세요.
> 3. 각 항목에 명시된 **[실행 명령]**을 터미널에서 직접 실행하세요.
> 4. 테스트가 실패(Exit Code != 0)하면, 즉시 에러 로그(전체 트레이스)를 `TEST_LOG.md` 파일에 기록하고 스스로 원인을 분석하여 코드를 재수정하세요. (최대 5회 반복)
> 5. 테스트가 완전 통과(조건 충족)하면, 이 문서를 열어 해당 항목을 `[x]`로 변경시키고 변경 사항을 안내하세요.

## 📋 목차
- [실행 현황 요약](#실행-현황-요약)
- [Phase 1: 데이터 무결성 검증](#phase-1-데이터-무결성-검증)
- [Phase 2: 백테스팅 엔진 검증](#phase-2-백테스팅-엔진-검증)
- [Phase 3: 고급 전략 검증](#phase-3-고급-전략-검증)
- [Phase 5: AI 리서치 및 텔레그램 검증](#phase-5-ai-리서치-및-텔레그램-검증)
- [디버깅 컨텍스트 (TEST_LOG)](#디버깅-컨텍스트-test_log)

---

## 실행 현황 요약
- **목적**: Phase 0-5 시스템의 데이터 수집, 처리, 백테스팅, 시각화 및 AI 리서치 기능 자동화 검증
- **최종 업데이트**: 2026-03-05 (Ralph Loop 포맷 리팩토링)

---

## Phase 1: 데이터 무결성 검증

- [ ] **1. 대차대조표 등식 검증**
  - **설명**: 수집된 재무 데이터 중 `자산 = 부채 + 자본` 등식이 성립하지 않는 이상 데이터가 있는지 확인합니다.
  - **종료 조건**: 반환되는 데이터베이스 레코드 수가 0이어야 합니다.
  - **[실행 명령]**: (해당 쿼리 또는 API 검증 스크립트 작성 후 실행 - 예: DB 조회 또는 검증 유틸 테스트)
- [ ] **2. 핵심 재무 지표(PER/PBR) Null 체크**
  - **설명**: 삼성전자(005930) 등 시총 상위 종목의 최신 기준 지표가 누락(`null`)되지 않았는지 확인.
  - **종료 조건**: HTTP 200 OK 및 응답 JSON 내부 `per`, `pbr` 필드가 존재해야 함.
  - **[실행 명령]**: `curl -s http://localhost:8080/api/v1/finance/005930 | jq '.data.per'`

## Phase 2: 백테스팅 엔진 검증

- [ ] **3. Value 전략 (단일 팩터) 구동 검증**
  - **설명**: 백테스트 엔진이 정상적으로 Value 전략 객체를 로드하여 수익률(Return) 계산 및 거래(Trade) 이력을 남기는지 확인합니다.
  - **종료 조건**: JUnit 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*ValueStrategyTest*"`
- [ ] **4. 거래 제약 방어막 (Foolproof) 검증**
  - **설명**: 비중 총합이 100%를 초과하거나 마이너스 자산 편입 시도를 엔진이 `IllegalArgumentException`으로 차단하는지 확인합니다.
  - **종료 조건**: Invalid Parameter 주입 시 예외 발생 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*PortfolioValidatorTest*"`

## Phase 3: 고급 전략 검증

- [ ] **5. 멀티팩터 Z-Score 정규화 로직 검증**
  - **설명**: 가치+모멘텀+우량성 팩터 스코어링 시 평균과 표준편차를 기반으로 Z-Score가 정확히 계산되는지 검증합니다.
  - **종료 조건**: 계산 결과 값과 기대 값 일치 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*FactorScoringTest*"`
- [ ] **6. 듀얼 모멘텀 자산 배분 전환 검증**
  - **설명**: 시장 하락장 조건에서 주식 비중을 강제로 0%로 만들고 현금 100% 보유 상태로 리밸런싱하는지 확인합니다.
  - **종료 조건**: 하락장 Simulation Mocking 시, `PortfolioHolding`의 주식 비중 검증 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*DualMomentumTest*"`

## Phase 5: AI 리서치 및 텔레그램 검증

- [ ] **7. 텔레그램 `/report` 명령어 포맷팅 검증**
  - **설명**: 잘못된 형식의 종목명/코드를 입력했을 때, 봇 파싱 로직이 죽지 않고 안내 메시지를 던지는지 검증합니다. (Foolproof)
  - **종료 조건**: Invalid Input 처리 관련 단위 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*TelegramBotParserTest*"`
- [ ] **8. 실시간 지표 알림(Alert) 발송 트리거 검증**
  - **설명**: Alert 조건 (예: PER 10 이하) 달성 시 메시징 큐(Event)가 정상 발행되는지 확인합니다.
  - **종료 조건**: 알림 Event Publisher Mocking 단위 테스트 통과.
  - **[실행 명령]**: `./gradlew :services:stock-ai:test --tests "*AlertTriggerTest*"`

---

## 디버깅 컨텍스트 (TEST_LOG)
> **AI 작업자 필독**: 이 항목 아래에 과거 실패했던 주요 원인이나 임시 메모를 남겨두세요. 루프 도중 무한 반복을 막기 위한 '기억 상자'입니다.
- (현재 디버깅 이슈 없음)

