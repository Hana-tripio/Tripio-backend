# TRIPIO PostgreSQL ERD v1

## 작성 목적

이 문서는 API 명세를 정하기 전에 백엔드 A/B/C 개발자가 먼저 합의해야 할 ERD 초안입니다. 실제 API DTO는 이 ERD를 기준으로 화면 요구사항과 백엔드 서비스 흐름을 맞춘 뒤 작성하는 것을 권장합니다.

현재 문서 기준은 PostgreSQL입니다. Markdown 백엔드 문서 안에는 DB명이 명시되어 있지 않았지만, 프로젝트 방향이 PostgreSQL이라면 이 설계를 그대로 PostgreSQL 물리 ERD 초안으로 사용할 수 있습니다.

## 설계 원칙

- `User`, `Region`, `Place`, `TravelEtf`, `DesignSession`, `TripExecution`을 핵심 축으로 둡니다.
- `Place`는 ETF, AI 설계, 인증에서 모두 쓰이므로 독립 기준 데이터로 둡니다.
- 저장된 ETF 일정과 설계 중인 일정은 분리합니다.
- 따라가기는 단순 social action이 아니라 `DesignSession` 생성 액션으로 봅니다.
- 카드 결제 내역은 mock과 실제 하나카드 연동을 모두 받을 수 있게 `CardPaymentEvent`로 추상화합니다.
- 리워드 포인트 이력과 지역 기여도 계산 결과는 분리합니다.
- 태그는 검색/필터를 위해 별도 테이블로 정규화합니다.

## 개발자별 담당 범위

### 개발자 A: ETF / 탐색 / 소셜

주요 테이블:

- `travel_etfs`
- `travel_etf_style_tags`
- `etf_itinerary_days`
- `etf_itinerary_items`
- `etf_portfolio_ratios`
- `etf_likes`
- `etf_scraps`
- `etf_follows`
- `etf_ratings`

주의할 점:

- `travel_etfs.like_count`, `scrap_count`, `follow_count`, `verification_count`, `rating_average`는 조회 성능용 집계 캐시입니다.
- 실제 원천 데이터는 각각 `etf_likes`, `etf_scraps`, `etf_follows`, `trip_executions`, `etf_ratings`입니다.
- 첫 구현에서는 집계 컬럼을 서비스 로직에서 동기화하거나, 조회 시 count로 계산할지 팀에서 선택해야 합니다.

### 개발자 B: AI 여행 설계 / 따라가기 / 일정 편집

주요 테이블:

- `design_sessions`
- `design_session_style_tags`
- `design_itinerary_days`
- `design_itinerary_items`

주의할 점:

- `design_sessions.source_etf_id`는 기존 ETF 따라가기 플로우에서 원본 ETF를 가리킵니다.
- `design_sessions.saved_etf_id`는 설계 세션을 최종 저장해 생성된 ETF를 가리킵니다.
- `design_itinerary_items.source_item_id`는 원본 ETF 일정 아이템을 가리킵니다.
- `source_item_id`와 `is_core`를 이용해 코어 코스 유지율을 계산할 수 있습니다.
- `ai_generated_raw_json`은 AI 응답 원본 저장용이며, 백엔드 DTO 검증을 통과한 뒤 저장해야 합니다.

### 개발자 C: 사용자 / 지역 / 장소 / 인증 / 리워드

주요 테이블:

- `users`
- `user_profiles`
- `user_preferred_style_tags`
- `regions`
- `places`
- `festivals`
- `trip_executions`
- `card_payment_events`
- `trip_verifications`
- `reward_histories`
- `local_contributions`

주의할 점:

- `regions`는 self relation으로 `충청남도 > 공주` 같은 계층 구조를 표현합니다.
- `places`는 모든 도메인의 기준 데이터이므로 가장 먼저 seed가 필요합니다.
- `card_payment_events.payment_provider`는 MVP에서는 `MOCK`, 실제 연동 후에는 `HANA_CARD`를 사용합니다.
- `trip_verifications`는 결제 이벤트와 장소/지역/시간 매칭 결과를 저장합니다.

## 테이블 상세

### 1. users

사용자 계정 기본 정보입니다.

주요 컬럼:

- `id`: 사용자 PK
- `email`: 로그인 이메일, unique
- `password_hash`: 비밀번호 해시
- `nickname`: 서비스 표시 이름
- `travel_style`: 대표 여행 성향
- `reward_point`: 현재 보유 리워드 포인트 캐시
- `status`: `ACTIVE`, `INACTIVE`, `WITHDRAWN`

관계:

- `users 1:1 user_profiles`
- `users 1:N travel_etfs`
- `users 1:N design_sessions`
- `users 1:N trip_executions`

### 2. user_profiles

사용자 프로필 확장 정보입니다.

주요 컬럼:

- `user_id`: users FK, unique
- `profile_image_url`
- `introduction`
- `preferred_region_id`: 선호 지역

### 3. regions

지도, 지역 리포트, PER 점수의 기준 데이터입니다.

주요 컬럼:

- `parent_region_id`: 상위 지역 FK
- `name`: 지역명
- `region_type`: `COUNTRY`, `PROVINCE`, `CITY`, `COUNTY`
- `latitude`, `longitude`: 지도 중심 좌표
- `per_score`: 지역 PER 점수
- `local_contribution_base_score`: 지역 기여도 기본 점수

예시:

- 충청남도
- 충청북도
- 대전
- 세종
- 공주
- 부여
- 천안
- 청주
- 단양

### 4. places

여행 장소, 로컬 매장, 전통시장, 숙소, 식당, 카페, 액티비티 기준 데이터입니다.

주요 컬럼:

- `region_id`: 지역 FK
- `category`: `LODGING`, `RESTAURANT`, `CAFE`, `ACTIVITY`, `FESTIVAL`, `MARKET`, `TRANSPORT`, `ETC`
- `is_local`: 로컬 매장 여부
- `is_traditional_market`: 전통시장 여부
- `is_core_spot`: 코어 스팟 여부
- `is_verifiable`: 카드 인증 후보 장소 여부
- `estimated_cost`: 예상 비용

사용처:

- ETF 일정
- 설계 세션 일정
- AI 후보 장소
- 카드 결제 인증 장소 매칭

### 5. style_tags

여행 스타일 태그 기준 테이블입니다.

예시:

- 힐링
- 역사
- 로컬푸드
- 감성
- 액티비티
- 시장

매핑 테이블:

- `user_preferred_style_tags`
- `travel_etf_style_tags`
- `design_session_style_tags`

### 6. travel_etfs

TRIPIO의 중심 엔티티입니다. 저장된 여행 ETF의 본문 정보를 담습니다.

주요 컬럼:

- `owner_id`: ETF 소유자
- `region_id`: 대상 지역
- `title`
- `summary`
- `status`: `DRAFT`, `PRIVATE`, `PUBLIC`, `ARCHIVED`
- `total_budget`
- `duration_days`
- `local_contribution_score`
- `region_value_score`
- `expected_reward`
- `thumbnail_url`
- `like_count`
- `scrap_count`
- `follow_count`
- `verification_count`
- `rating_average`

주의:

- count 계열 컬럼은 성능용 캐시입니다.
- 정합성을 위해 social action 생성/삭제 시 같이 갱신하거나 batch로 재계산해야 합니다.

### 7. etf_itinerary_days

저장된 ETF의 Day 단위 일정입니다.

주요 제약:

- `(travel_etf_id, day_number)` unique
- `day_number > 0`

### 8. etf_itinerary_items

저장된 ETF의 장소별 일정 아이템입니다.

주요 컬럼:

- `itinerary_day_id`
- `place_id`
- `sequence`
- `start_time`, `end_time`
- `estimated_cost`
- `is_core`
- `memo`

주의:

- `(itinerary_day_id, sequence)` unique
- `is_core = true`인 아이템은 따라가기 코어 유지율 계산 기준입니다.

### 9. etf_portfolio_ratios

ETF 예산 포트폴리오 비율입니다.

주요 컬럼:

- `lodging_ratio`
- `food_ratio`
- `cafe_ratio`
- `activity_ratio`
- `festival_ratio`
- `local_store_ratio`
- `transport_ratio`

주의:

- v1에서는 각 비율의 합계가 100인지 DB 제약으로 강제하지 않았습니다.
- 소수점 오차와 비즈니스 규칙 변경 가능성 때문에 서비스에서 검증하는 것을 추천합니다.

### 10. design_sessions

AI 생성, 따라가기, 일정 편집 중인 임시 설계 세션입니다.

주요 컬럼:

- `user_id`
- `source_etf_id`: 기존 ETF 따라가기일 때 원본 ETF
- `saved_etf_id`: 저장 완료 후 생성된 ETF
- `region_id`
- `status`: `CREATED`, `AI_GENERATED`, `EDITING`, `SAVED_AS_ETF`, `CANCELLED`
- `input_budget`
- `input_days`
- `input_people_count`
- `input_companion_type`
- `local_contribution_enabled`
- `ai_generated_raw_json`
- `core_retention_rate`

### 11. design_itinerary_days

설계 중인 Day 단위 일정입니다.

주요 제약:

- `(design_session_id, day_number)` unique

### 12. design_itinerary_items

설계 중인 장소별 일정 아이템입니다.

주요 컬럼:

- `design_itinerary_day_id`
- `place_id`
- `source_item_id`: 원본 ETF 일정 아이템
- `sequence`
- `start_time`, `end_time`
- `estimated_cost`
- `is_core`
- `ai_reason`

주의:

- 기존 ETF 따라가기에서 복사된 아이템은 `source_item_id`를 가집니다.
- 사용자가 삭제하거나 교체하면 코어 유지율 계산에 반영됩니다.

### 13. etf_likes

ETF 좋아요입니다.

제약:

- `(user_id, travel_etf_id)` unique

### 14. etf_scraps

ETF 저장/스크랩입니다.

제약:

- `(user_id, travel_etf_id)` unique

### 15. etf_follows

ETF 따라가기 기록입니다.

주요 컬럼:

- `user_id`
- `source_etf_id`
- `design_session_id`

주의:

- 따라가기는 `DesignSession` 생성과 함께 발생합니다.
- `design_session_id`는 unique로 두어 하나의 follow 기록이 하나의 설계 세션에 대응하게 했습니다.

### 16. etf_ratings

ETF 만족도/별점입니다.

제약:

- `(user_id, travel_etf_id)` unique
- `score BETWEEN 1 AND 5`

### 17. trip_executions

사용자가 ETF 기반으로 시작한 실제 여행 수행 기록입니다.

주요 컬럼:

- `user_id`
- `travel_etf_id`
- `status`: `READY`, `IN_PROGRESS`, `COMPLETED`, `PARTIALLY_VERIFIED`, `FAILED_VERIFICATION`
- `started_at`
- `ended_at`

### 18. card_payment_events

mock 또는 하나카드 결제 내역 이벤트입니다.

주요 컬럼:

- `user_id`
- `payment_provider`: `MOCK`, `HANA_CARD`
- `merchant_name`
- `merchant_address`
- `region_id`
- `category`
- `amount`
- `paid_at`
- `latitude`, `longitude`
- `external_payment_id`
- `raw_payload`

주의:

- 실제 카드 API 원본 응답은 `raw_payload`에 저장할 수 있지만, 민감정보는 저장하지 않거나 마스킹해야 합니다.
- `payment_provider + external_payment_id`는 중복 결제 이벤트 방지용 unique입니다.

### 19. trip_verifications

카드 결제 이벤트와 여행 일정/장소/지역 매칭 결과입니다.

주요 컬럼:

- `trip_execution_id`
- `payment_event_id`
- `place_id`
- `verification_type`: `CARD_PAYMENT`, `LOCAL_STORE_PAYMENT`, `TRADITIONAL_MARKET_PAYMENT`, `REGION_PAYMENT`
- `status`: `PENDING`, `VERIFIED`, `PARTIALLY_VERIFIED`, `REJECTED`
- `matched_score`
- `reason`
- `verified_at`

### 20. reward_histories

사용자 리워드 포인트 증감 이력입니다.

주요 컬럼:

- `user_id`
- `trip_execution_id`
- `region_id`
- `reward_type`
- `point`
- `reason`
- `calculated_at`

주의:

- `point`는 적립이면 양수, 차감/정정이면 음수를 허용합니다.

### 21. local_contributions

지역 기여도 계산 결과입니다.

주요 컬럼:

- `user_id`
- `region_id`
- `trip_execution_id`
- `contribution_score`
- `local_payment_amount`
- `traditional_market_payment_amount`
- `verified_place_count`
- `calculated_at`

## API 설계 전에 팀에서 확정할 질문

1. `travel_etfs`의 count/rating 캐시를 실시간 갱신할지, 조회 시 계산할지
2. `style_tags`를 관리자 seed로 고정할지, 사용자 입력 태그를 허용할지
3. `travel_etfs.status = DRAFT`를 실제 저장 ETF에도 사용할지, 설계 단계는 `design_sessions`에만 둘지
4. `trip_executions` 시작 시점에 ETF snapshot을 저장할지, 항상 최신 ETF를 참조할지
5. 카드 결제 이벤트의 `raw_payload` 저장 범위와 개인정보 마스킹 기준
6. 리워드 포인트 정정/취소 정책
7. 지역 기여도 점수를 여행별로만 저장할지, 월별 집계 테이블을 별도로 둘지
8. AI 요청/응답 로그를 `design_sessions.ai_generated_raw_json`만으로 충분히 볼지, 별도 `ai_generation_logs` 테이블이 필요한지

## v2에서 검토할 확장 테이블

아래 테이블은 v1 MVP ERD에서는 제외했지만, 기능이 커지면 추가를 검토합니다.

- `ai_generation_logs`: AI 요청/응답/실패 사유/토큰 사용량 기록
- `travel_etf_snapshots`: 여행 시작 시점의 ETF 일정 snapshot
- `monthly_reward_summaries`: 월 단위 리워드 집계
- `region_heatmap_snapshots`: 지도 히트맵용 사전 계산 결과
- `notifications`: 알림
- `refresh_tokens`: refresh token 또는 device session 관리
