# 업종(Sector) 데이터 수집 자동화 실행 계획

## 1. 개요
현재 `CorpDetail`에 추가된 `sector` 필드를 수동으로 입력하는 대신, OpenDART API의 기업개황 정보를 활용하여 자동으로 수집하고 `SectorType` Enum으로 매핑하여 저장하는 자동화 프로세스를 구축합니다.

## 2. 주요 구성 요소

### 2.1. DartClient (stock-corp 서비스)
OpenDART API와의 통신을 담당하는 HTTP 클라이언트를 구현합니다.
- **대상 API**: 기업개황 (`https://opendart.fss.or.kr/api/company.json`)
- **인증**: 기존 환경 변수 `${DART_API_KEY}` 활용
- **기능**: `corp_code`(8자리)를 입력받아 업종명(`induty_nm`) 반환

### 2.2. Sector Mapping 로직 (stock-common 모듈)
DART에서 제공하는 텍스트 형태의 업종명을 시스템에서 정의한 `SectorType`으로 변환합니다.
- **구현 위치**: `SectorType.fromDescription(String description)`
- **매핑 방식**: 키워드 포함 여부 기반 (예: "의약품", "제약" 포함 시 `HEALTHCARE` 매핑)

### 2.3. Sector 수집 배치 (SectorUpdateBatch)
기존 DB에 저장된 기업들을 순회하며 업종 정보를 업데이트하는 배치 작업을 구현합니다.
- **Reader**: `TB_CORP_INFO`에서 `corp_code` 목록 조회
- **Processor**: `DartClient`를 호출하여 업종명 획득 및 매핑
- **Writer**: `TB_CORP_DETAIL`의 `sector` 컬럼 업데이트

## 3. 상세 구현 단계

### Step 1: DartClient 구현
- `stock-corp` 서비스에 `RestClient` 기반의 클라이언트 생성
- API 호출 결과 파싱을 위한 DTO 정의

### Step 2: 매핑 로직 고도화
- `SectorType` Enum의 `fromDescription` 메서드에 한국표준산업분류(KSIC) 기반 키워드 상세 매핑 추가

### Step 3: 배치 작업 구성
- `stock-corp` 서비스 내부에 `SectorUpdateJob` 생성
- 대량 호출 시 DART API 속도 제한(Rate Limit)을 고려한 지연(Delay) 또는 청크 단위 조절 로직 포함

### Step 4: 기존 수집 프로세스 통합
- `CorpInfoService`에서 새로운 기업 정보를 수집할 때 자동으로 `DartClient`를 호출하여 `CorpDetail`까지 생성하도록 로직 통합

## 4. 기대 효과
- **데이터 정확성**: 금융감독원 공식 데이터를 활용하여 업종 분류의 신뢰도 확보
- **운영 효율성**: 수천 개의 상장사 업종 정보를 수작업 없이 최신 상태로 유지
- **전략 정교화**: 정확한 업종 필터링을 통해 퀀트 전략의 품질 향상
