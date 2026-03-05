# Phase 5 실행 계획 (Action Plan): AI 인사이트 및 리서치 시스템

## 1. 개요
본 문서는 Phase 5(AI 인사이트 및 리서치)의 상세 구현 계획을 정의합니다. 퀀트 데이터(정량)와 LLM/뉴스(정성) 데이터를 결합하여 지능형 투자 리포트를 생성하고, 이를 텔레그램을 통해 사용자에게 개인화된 형태로 전달하는 것을 목표로 합니다.

## 2. 시스템 아키텍처 (stock-ai 모듈 신설)

새로운 마이크로서비스 **`stock-ai`**를 구축하여 기존 서비스들과 격리된 AI 환경을 구성합니다.
- **Service Name**: `stock-ai`
- **AI Engine**: Spring AI (Google Gemini 통합)
- **Database**: `stock_ai_db` (PostgreSQL 16 + pgvector)
- **Vector Storage**: 
  - **PostgreSQL (pgvector)**: 업계 표준 벡터 검색 엔진 (HNSW 인덱스 활용) - 단일 표준으로 채택하여 구현 복잡도 최소화 및 고성능 검색 확보
- **Data Source**: 타 서비스 REST API 연동 (데이터 결합)
- **Delivery**: Telegram Bot API (Spring WebClient 연동)

---

## 3. 로컬 리소스 관리 전략 (M1 8GB RAM 최적화)

본 프로젝트는 맥북 M1 8GB 환경에서도 원활히 구동될 수 있도록 엄격한 리소스 제한을 적용합니다.

### 3.1 컨테이너 메모리 제한 (Limits)
| 서비스 명 | 권장 메모리 제한 (Limit) | JVM Heap 설정 (Xmx) | 비고 |
| :--- | :--- | :--- | :--- |
| **stock-ai** | **384MB** | **256MB** | Spring AI 오버헤드 고려 최소치 |
| **stock-ai-db** | **256MB** | - | PostgreSQL (pgvector) 경량화 설정 적용 |

### 3.2 DB 경량화 설정 (PostgreSQL)
메모리 점유를 줄이기 위해 다음 설정을 `docker-compose`에 반영합니다:
- `max_connections`: 20 (기본 100에서 축소)
- `shared_buffers`: 64MB (데이터 캐싱 최소화)
- `work_mem`: 4MB (정렬 작업 메모리 최소화)

### 3.3 운영 팁
- **선택적 구동**: AI 기능 개발 시에는 `stock-strategy` 등 현재 작업과 무관한 서비스를 일시 정지하여 가용 메모리 확보.
- **Docker Desktop 자원 할당**: Docker에 할당된 전체 메모리가 6GB를 넘지 않도록 설정 권장 (OS 및 IDE 공간 확보).

---

## 4. 상세 구현 단계

### 4.1 stock-ai 모듈 초기화 및 인프라 (1주차) [완료]
- [x] **모듈 생성**: `services/stock-ai` 디렉토리 신설 및 `build.gradle` 설정.
- [x] **Spring AI 통합**: Google Gemini API 연동을 위한 의존성 추가.
- [x] **Vector DB 스키마 설계**: PostgreSQL 16 (pgvector) 기준으로 공시 본문 임베딩 저장을 위한 `vector` 타입 컬럼 테이블 정의.
- [x] **보안 관리 (Secrets)**:
  - `.env` 파일을 통한 `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` 관리.
  - 확장성을 위해 `TelegramBotProperty` 클래스를 구성하여 다중 봇 지원 구조 설계.
- [x] **텔레그램 명령 처리기 (Command Pattern)**:
  - `/start`, `/report [종목명]`, `/summary` 등 명령어를 처리할 수 있는 구조 구축.

### 4.2 데이터 전처리 및 분석 엔진 (2주차) [완료]
- [x] **데이터 수집 및 동기화 전략 (Event-Driven & Polling)**:
  - [x] **최신 공시 폴링**: OpenDART의 '공시서류 검색' API를 주기적(15~30분)으로 호출하여 전체 상장사의 최신 공시 목록 수집.
  - [x] **재무 업데이트 감지**: 공시 유형이 **정기공시(`pblntf_ty=A`)**이면서 '사업/분기/반기보고서'인 경우, 해당 기업의 최신 재무 데이터 업데이트 이벤트 발생.
  - [x] **정밀 수집 및 동기화**: 감지된 기업에 한해 '단일/다중회사 주요재무사항' API를 호출하여 매출, 영업이익 등 핵심 지표를 즉시 수집하고 `stock-finance` 서비스와 동기화.
  - [x] **기존 서비스 기능 확장 (`stock-finance`)**: 특정 기업의 재무 데이터만 선별적으로 수집할 수 있도록 `CorpFinanceController`의 `/batch/corp-fin` API에 `corpCode` 파라미터 추가 및 배치 연동 로직 고도화.
  - [x] **중복 방지 및 필터링**: `rcept_no`(접수번호)를 식별자로 사용하여 중복 수집을 방지하며, 관심/보유 종목 위주로 우선 분석하여 리소스 낭비 최소화.
- [x] **Prompt Engineering**: DB 내 재무제표(JSON)를 LLM이 이해하기 쉬운 요약 텍스트로 변환하는 템플릿 개발.
- [x] **재무 스코어링**: AI가 판단한 수익성, 안정성, 성장성 점수 산출 로직.
- [x] **RAG (Retrieval-Augmented Generation)**:
  - [x] 선별된 DART 공시 본문 및 재무 지표를 파싱하여 벡터화 후 `pgvector` 저장.
  - [x] 질문에 적합한 공시 문맥과 재무 수치를 결합 추출하여 LLM 답변의 정확도 향상.

### 4.3 텔레그램 리포팅 시스템 (3주차) [완료]
- [x] **자동화 스케줄러**:
  - `08:30`: 당일 주요 일정 및 관심 종목 AI 요약 발송.
  - `16:00`: 장 마감 후 체결 강도 및 재무 특이점 보고서 발송.
- [x] **인터랙티브 봇**: 사용자가 종목명을 입력하면 즉시 현재가, 재무 상태, AI 의견을 한눈에 볼 수 있는 카드형 메시지 응답.
- [x] **시각화 및 요약 메시지 (Image + Text)**: 
  - `jFreeChart` 등을 활용해 재무/주가 추이 차트를 가벼운 이미지 파일로 생성.
  - 텔레그램의 `sendPhoto` API와 `HTML` 모드를 조합하여 이미지와 함께 상세 분석 본문을 한 개의 메시지로 묶어서 발송 (PDF 대비 리소스 점유율 80% 감소).

### 4.4 실시간 금융 알림 및 고도화 (4주차) [완료]
- [x] **조건부 지표 알림 (Alert System)**:
  - 사용자가 텔레그램으로 `/alert [종목코드] [지표] [조건]` (예: `/alert 005930 PER 10`) 입력 시 DB에 저장.
  - 배치 프로세스가 주기적으로 지표를 확인하여 조건 충족 시 알림 발송.
- [x] **텔레그램 기반 관심종목(Watchlist) 관리**:
  - `/watch [종목코드]`, `/unwatch [종목코드]` 명령어로 나만의 관심 종목 리스트 관리.
  - 정기 리포트 발송 시 관심 종목 위주로 우선 분석 제공.
- [x] **대화형 RAG 고도화**:
  - 단순 질문 답변을 넘어, 여러 공시 문서를 교차 분석하여 리포트 간의 차이점이나 시계열적 변화를 추적하는 심층 분석 기능. (이미 PromptService와 AiInsightService에 반영됨)
- [x] **웹 대시보드 연동 (Web UI Support)**:
  - `Settings` 화면에서 관심 종목 리스트 조회 및 삭제 기능 구현.
  - 현재 활성화된 지표 알림 목록 시각화 및 모니터링 상태 표시.
  - Gateway 라우팅 설정을 통한 프론트엔드-AI 서비스 간 REST API 통신 구축.

---

## 5. API 키 및 설정 관리 (Security)

### 5.1 `.env` 설정 항목
```properties
# Telegram Bot Settings
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_personal_chat_id
TELEGRAM_BOT_NAME=StockMsaBot

# AI Provider Settings
SPRING_AI_GEMINI_API_KEY=your_gemini_key_here

# AI Database Settings (PostgreSQL + pgvector)
AI_DB_USER=ai_user
AI_DB_PASSWORD=ai_pass
STOCK_AI_DB_URL=jdbc:postgresql://localhost:5432/stock_ai?useSSL=false
```

### 5.2 확장성 고려 (Multi-Tenant)
개인용 확장을 위해 `TelegramBotService`는 특정 토큰에 종속되지 않고, 인수로 전달받은 토큰과 Chat ID로 메시지를 보낼 수 있는 `sendCustomMessage(token, chatId, text)` 메서드를 포함하도록 설계합니다.

---

## 6. 기대 효과
- **정보 격차 해소**: 수천 개의 종목 중 재무/주가 측면에서 특이점이 발생한 종목을 AI가 먼저 찾아줌.
- **접근성 향상**: 대시보드에 접속하지 않고도 텔레그램 대화만으로 종목 분석 및 포트폴리오 관리 가능.
- **개인 비서화**: 나만의 투자 원칙을 AI에게 학습시켜 개인 맞춤형 리서치 환경 구축 가능.
