# Phase 4 실행 계획 (Action Plan): 프론트엔드 대시보드 구축

## 1. 개요
본 문서는 `docs/Implementation_Roadmap.md`에 정의된 Phase 4 (프론트엔드 대시보드)의 구체적인 실행 계획을 정의합니다. 기존에 구축된 Spring Boot 기반의 MSA 백엔드 API(데이터 수집, 검증, 백테스팅, 고급 퀀트 전략)를 활용하여, 사용자가 직관적으로 데이터를 조회하고 시뮬레이션을 실행할 수 있는 웹 기반의 시각화 대시보드를 구축하는 것을 목표로 합니다.

## 2. 아키텍처 및 기술 스택

프론트엔드는 백엔드 서비스들과 독립적인 생명주기를 가지는 **별도의 SPA(Single Page Application)** 프로젝트로 구성되며, Nginx를 통해 정적 파일로 서빙되어 Docker Compose 네트워크에 통합됩니다.

### 2.1 핵심 기술 스택
- **코어 프레임워크**: React 18, TypeScript, Vite (빠른 빌드 및 HMR 지원)
- **상태 관리 및 데이터 페칭**: 
  - 서버 상태(API): TanStack Query (React Query) - 캐싱 및 비동기 상태 관리
  - 전역 상태(UI): Zustand - 테마, 알림, 사용자 설정 등 가벼운 상태 관리
- **UI 컴포넌트 및 스타일링**: 
  - CSS 프레임워크: Tailwind CSS
  - 컴포넌트 라이브러리: shadcn/ui (Radix UI 기반, 커스터마이징 용이)
  - 아이콘: Lucide React
- **데이터 시각화 (차트)**:
  - 주가/캔들스틱 차트: Lightweight Charts (TradingView)
  - 통계/비중 차트: Recharts (라인, 파이, 레이더 차트 등)
- **라우팅**: React Router v6
- **인프라**: Nginx (Alpine 기반 Docker 컨테이너)

### 2.2 UI/UX 디자인 원칙 (Robinhood-Style)
본 대시보드는 로빈후드(Robinhood) 앱과 유사한 직관적이고 미니멀한 모던 금융 UI를 지향합니다.
- **반응형 웹 (Responsive Design)**: 모바일(MO) 기기에서의 한 손 조작과 작은 화면에서의 가독성을 최우선으로 고려하는 모바일 퍼스트(Mobile-First) 접근법을 사용하며, 데스크탑(PC) 화면에서는 남는 공간을 활용해 더 많은 데이터 그리드를 보여주도록 유연하게 확장됩니다.
- **다크 모드 기본 (Dark Mode First)**: 깊은 블랙 배경(`bg-black` 또는 `bg-zinc-950`)을 사용하여 데이터 집중도를 높이고 눈의 피로를 최소화.
- **색상 대비 (High Contrast)**: 상승장(수익)은 네온 그린(`text-green-400`), 하락장(손실)은 네온 레드(`text-red-500`)를 강렬하게 사용하여 즉각적인 상태 인지.
- **미니멀리즘 (Minimalism)**: 불필요한 테두리(Borders), 섀도우, 복잡한 그리드 라인을 제거하고 여백(Margin/Padding)을 통해 요소를 구분.
- **타이포그래피 (Typography)**: 총 자산, 누적 수익률 등 핵심 데이터는 매우 크고 굵은 폰트(Hero Typography)로 최상단에 배치하여 임팩트 부여.
- **인터랙션 (Smooth Interaction)**: 차트 호버 시 부드러운 툴팁 전환, 탭 이동 시 애니메이션(Framer Motion 연계 고려) 적용.

### 2.3 시스템 연동 구조
- 프론트엔드 컨테이너(`stock-dashboard`: 3000 포트)는 오직 정적 파일 서빙만 담당합니다.
- 클라이언트 브라우저는 API 요청 시 API Gateway(`http://localhost:8080/api/v1/...`)를 거쳐 백엔드 마이크로서비스(`stock-corp`, `stock-price`, `stock-finance`, `stock-strategy`)와 통신합니다.

---

## 3. 주요 기능 및 화면 구성

### 3.1 대시보드 홈 (Dashboard Home)
- **목표**: 시스템 전체의 요약 정보 제공
- **화면 구성**:
  - **Hero Section**: 초대형 폰트로 현재 전체 백테스트 평균 수익률 또는 코스피 지수 렌더링 (그린/레드 컬러 테마 즉각 반영).
  - KOSPI/KOSDAQ 주요 지수 미니 스파크라인(Sparkline) 차트 (그리드 라인 없는 심플한 라인).
  - 최근 수집된 데이터(주가, 재무) 건수 및 검증 통과율(VERIFIED %) 요약 카드 (미니멀 카드 레이아웃).
  - 최근 실행된 백테스트 결과 Top 3 요약 테이블.

### 3.2 단일 백테스팅 시뮬레이터 (Backtesting Simulator)
- **목표**: 사용자가 전략 파라미터를 입력하고 실행 결과를 시각적으로 분석
- **화면 구성**:
  - **결과 요약 (Top Panel)**: 로빈후드 스타일의 거대한 총 자산(Final Value) 및 수익률(CAGR) 타이포그래피.
  - **자산 변동 차트 (Main Chart)**: 배경 그리드가 생략되고, 오직 포트폴리오의 자산 궤적만 부드러운 라인(또는 Area)으로 그려지는 차트. 호버 시 십자선(Crosshair)과 툴팁 노출.
  - **입력 폼 (Side/Bottom Panel)**: 슬라이더(Slider)나 깔끔한 토글(Toggle) 스위치 형태의 shadcn/ui 컴포넌트를 적극 활용하여 직관적인 파라미터(자본금, 제약조건 등) 조작.
  - **포트폴리오 스냅샷**: 특정 일자의 보유 종목 및 비중 파이 차트, 거래 내역 데이터 그리드 (테두리 없는 깔끔한 테이블).

### 3.3 전략 비교 및 최적화 분석 (Strategy Comparison & Grid Search)
- **목표**: 여러 시뮬레이션 결과를 비교하고 최적의 파라미터를 탐색
- **화면 구성**:
  - **다중 전략 렌더링**: 여러 백테스트 ID를 입력받아 누적 수익률 차트를 겹쳐서 표시 (비교 차트)
  - **그리드 서치 랭킹 보드**: `POST /optimize` 실행 후 반환된 여러 조합 중 상위 10개(CAGR, MDD 기준 등)를 정렬하여 보여주는 데이터 테이블
  - **지표 레이더 차트**: 선택한 전략들의 강점/약점(수익성, 안정성, 승률 등)을 레이더 차트로 비교 시각화

### 3.4 데이터 품질 및 수집 모니터링 (Data Quality Monitoring)
- **목표**: 배치 작업(수집/검증)의 상태 및 오류 모니터링
- **화면 구성**:
  - 날짜별/서비스별 API 호출 성공/실패율 타임라인
  - 대차대조표 등식 검증 실패 등 `ERROR_MISSING`, `ERROR_IDENTITY` 종목 리스트 뷰어

---

## 4. 단계별 구현 계획 (Phased Implementation Plan)

### 1주차: 프로젝트 초기화 및 레이아웃 구성
- `frontend` 모듈(디렉토리) 생성 및 Vite + React + TS 프로젝트 스캐폴딩
- Tailwind CSS 및 shadcn/ui 설정
- 공통 레이아웃(Sidebar, Header, Main Content Area) 및 테마(Dark/Light) 구성
- React Router 설정 및 빈 페이지 라우팅 연결
- Nginx Dockerfile 작성 및 `docker-compose.yaml` 통합

### 2주차: API 연동 기반 및 모니터링 화면 구현
- Axios 설정 및 React Query 인스턴스 세팅 (API Gateway 연동, CORS 해결)
- 백엔드 DTO에 대응하는 TypeScript 인터페이스 타입 정의
- **데이터 품질 모니터링 화면** 개발: 검증 통계 데이터 연동 및 시각화
- 공통 컴포넌트(데이터 테이블, 로딩 스피너, 에러 바운더리) 구현

### 3주차: 단일 백테스팅 시뮬레이터 개발
- 백테스팅 설정 입력 폼(Form) 컴포넌트 개발 (React Hook Form 활용)
- `POST /api/v1/strategy/backtest` 연동 및 결과 데이터 상태 관리
- KPI 지표 카드 및 누적 수익률 차트(Recharts) 구현
- 특정 날짜별 포트폴리오 구성 비중 파이 차트 연동

### 4주차: 고급 전략 시각화 및 최적화 도구 개발
- 다중 전략 비교 차트 구현 (여러 시계열 데이터 병합 렌더링 로직)
- 그리드 서치(`POST /optimize`) 결과를 렌더링하는 랭킹 보드 테이블 구현
- 종목별 기술적 지표 및 캔들스틱 차트 (Lightweight Charts) 추가
- UI/UX 폴리싱 및 모서리 케이스(에러 처리, 빈 데이터 화면) 보완

---

## 5. 인프라 변경 사항 및 고려사항
- **CORS (Cross-Origin Resource Sharing)**: Gateway(`stock-gateway`) 레벨에서 `localhost:3000` (또는 프론트엔드 호스트)의 CORS 요청을 허용하도록 설정 추가 필요.
- **API 인증 (Security)**: 현재 백엔드의 Basic Auth 방식을 토큰 기반(JWT)으로 전환하거나, 프론트엔드에서 세션/쿠키 기반으로 인증 토큰을 전달할 수 있는 체계 검토 (Phase 6 보안 강화와 연계).
- **Docker Compose**: `stock-dashboard` 서비스가 추가되며, 빌드 컨텍스트에 `frontend` 디렉토리가 포함됩니다.
