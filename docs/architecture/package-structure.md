# TRIPIO 백엔드 패키지 구조

## 목적

이 문서는 백엔드 팀원이 각자 맡은 도메인 코드를 어디에 작성해야 하는지 맞추기 위한 기준이다.

현재 단계에서는 JPA Entity 전체 초안을 공통으로 생성하지 않는다. 각 담당자가 API, 서비스 흐름, 연관관계 매핑 방식을 구체화하면서 자기 도메인의 Entity를 작성한다.

## 기본 원칙

- 도메인 중심 패키지 구조를 사용한다.
- 각 도메인은 동일한 하위 구조를 가진다.
- JPA Entity는 담당 도메인 개발자가 작성한다.
- 공통 설정, 보안, 응답, 예외 처리는 `global` 아래에서 관리한다.
- DB 테이블 구조는 Flyway 마이그레이션을 기준으로 한다.
- Hibernate `ddl-auto`는 `validate`를 유지한다.

## 최상위 패키지

```text
com.tripio
├── global
├── user
├── region
├── place
├── etf
├── design
├── discovery
├── social
├── verification
├── reward
└── ai
```

## 도메인별 하위 패키지

각 도메인은 아래 구조를 기본으로 사용한다.

```text
domain
├── controller
├── service
├── repository
├── entity
├── dto
└── type
```

### controller

HTTP API 엔드포인트를 둔다.

예:

```text
com.tripio.etf.controller
com.tripio.design.controller
com.tripio.reward.controller
```

### service

비즈니스 로직과 트랜잭션 경계를 둔다.

### repository

Spring Data JPA Repository 또는 QueryDSL 관련 클래스를 둔다.

### entity

JPA Entity를 둔다.

Entity 작성 시 주의:

- DB 컬럼명과 Flyway 스키마를 먼저 확인한다.
- enum 값은 DB `CHECK` 제약과 맞춘다.
- 연관관계는 담당 도메인 API 흐름을 기준으로 최소한만 매핑한다.
- 처음부터 모든 관계를 양방향으로 만들지 않는다.
- `cascade`, `orphanRemoval`, `fetch` 전략은 팀 리뷰 후 확정한다.

### dto

요청/응답 DTO를 둔다.

### type

상태값 enum, 도메인 enum을 둔다.

예:

```text
EtfStatus
DesignSessionStatus
TripExecutionStatus
VerificationStatus
RewardType
```

## 담당자별 추천 작업 범위

### 개발자 A: ETF / 탐색 / 소셜

주요 패키지:

```text
com.tripio.etf
com.tripio.discovery
com.tripio.social
```

주요 테이블:

```text
travel_etfs
travel_etf_style_tags
etf_itinerary_days
etf_itinerary_items
etf_portfolio_ratios
etf_likes
etf_scraps
etf_follows
etf_ratings
```

### 개발자 B: AI 여행 설계 / 따라가기 / 일정 편집

주요 패키지:

```text
com.tripio.design
com.tripio.ai
```

주요 테이블:

```text
design_sessions
design_session_style_tags
design_itinerary_days
design_itinerary_items
```

### 개발자 C: 사용자 / 지역 / 인증 / 리워드

주요 패키지:

```text
com.tripio.user
com.tripio.region
com.tripio.place
com.tripio.verification
com.tripio.reward
```

주요 테이블:

```text
users
user_profiles
user_preferred_style_tags
regions
places
festivals
trip_executions
card_payment_events
trip_verifications
reward_histories
local_contributions
```

## global 패키지

현재 `global`에는 공통 응답, 예외, 설정 코드가 있다.

```text
global
├── apiPayload
├── config
├── common
├── security
└── util
```

### apiPayload

공통 API 응답과 에러/성공 코드를 둔다.

응답 형식과 도메인별 코드 작성 규칙은 아래 문서를 따른다.

```text
docs/architecture/api-response-format.md
```

### config

Spring Security, Swagger, JPA, Web 설정 등을 둔다.

### common

여러 도메인에서 같이 쓰는 공통 타입이나 기반 클래스를 둔다.

### security

JWT 필터, 인증 provider, 인증 principal 등을 둔다.

### util

순수 유틸성 클래스를 둔다.

## 임시 test 도메인

현재 `com.tripio.domain.test`는 초기 헬스 체크와 공통 응답 검증을 위한 임시 패키지다.

실제 도메인 API가 생기면 아래 중 하나로 정리한다.

- health check 용도만 남긴다.
- actuator health로 대체한다.
- 개발 검증용 API라면 제거한다.

## Entity 작성 전 체크리스트

Entity를 추가하기 전에 아래를 확인한다.

```text
1. V1 Flyway 스키마의 테이블명과 컬럼명을 확인했는가?
2. enum 값이 DB CHECK 제약과 일치하는가?
3. 연관관계가 API 응답을 위해 꼭 필요한가?
4. 양방향 관계가 필요한 이유가 명확한가?
5. cascade 설정이 데이터 삭제 정책과 맞는가?
6. Repository 테스트를 같이 작성했는가?
```

## 새 파일 작성 예시

ETF 상세 조회 API를 만든다면 다음 위치를 사용한다.

```text
src/main/java/com/tripio/etf/controller/EtfController.java
src/main/java/com/tripio/etf/service/EtfService.java
src/main/java/com/tripio/etf/repository/TravelEtfRepository.java
src/main/java/com/tripio/etf/entity/TravelEtf.java
src/main/java/com/tripio/etf/dto/EtfDetailResponse.java
src/main/java/com/tripio/etf/type/EtfStatus.java
```
