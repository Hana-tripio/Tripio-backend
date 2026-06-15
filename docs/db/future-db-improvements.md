# TRIPIO DB 향후 개선 사항

## 문서 목적

이 문서는 `V1__init_tripio_schema.sql`의 현재 의미와 향후 DB 개선 후보를 팀원들이 같은 기준으로 이해하기 위해 작성한다.

`V1__init_tripio_schema.sql`은 단순 임시 SQL이 아니라, 현재 백엔드 기능 명세와 ERD 검토를 바탕으로 만든 TRIPIO 초기 개발 기준 스키마다. 다만 API, 인증 정책, 리워드 정책, AI 호출 정책이 구체화되면 이후 마이그레이션으로 확장한다.

## 현재 기준

현재 DB 스키마 관리 방식은 Flyway를 기준으로 한다.

```text
src/main/resources/db/migration/V1__init_tripio_schema.sql
```

ERD 원본 문서와 이미지는 백엔드 레포에서 관리한다.

```text
docs/db/erd/tripio-erd-postgresql-v1.md
docs/db/erd/tripio-erd-postgresql-v1.sql
docs/db/erd/tripio-erd-full-v1.png
docs/db/erd/tripio-erd-full-v1.svg
```

V1은 다음 목적을 가진다.

- 팀원이 동일한 PostgreSQL 스키마로 개발을 시작한다.
- Hibernate가 테이블을 자동 생성하지 않도록 한다.
- JPA 엔티티는 Flyway가 만든 DB 구조와 일치하는지만 검증한다.
- 추후 변경 사항은 `V2`, `V3` 마이그레이션으로 누적 관리한다.

현재 JPA 설정은 `ddl-auto: validate`다. 따라서 테이블 생성과 변경은 반드시 Flyway 마이그레이션으로 관리한다.

## V1 포함 범위

### User / Profile

- `users`
- `user_profiles`
- `user_preferred_style_tags`

포함 기능:

- 회원 계정 기본 정보
- 프로필 확장 정보
- 선호 지역
- 선호 여행 스타일 태그
- 현재 보유 리워드 포인트 캐시

### Region / Place / Festival

- `regions`
- `places`
- `festivals`
- `style_tags`

포함 기능:

- 계층형 지역 구조
- 지도 중심 좌표
- 지역 PER 점수
- 지역 기여도 기본 점수
- 여행 장소, 로컬 매장, 전통시장, 숙소, 식당, 카페, 액티비티
- 코어 스팟 여부
- 카드 인증 후보 장소 여부
- 지역 축제/행사 정보

### Travel ETF

- `travel_etfs`
- `travel_etf_style_tags`
- `etf_itinerary_days`
- `etf_itinerary_items`
- `etf_portfolio_ratios`

포함 기능:

- ETF 소유자
- ETF 대상 지역
- 공개/비공개/초안/보관 상태
- 예산, 일정 기간, 지역상생 점수, 지역가치 점수
- 예상 리워드
- 좋아요/스크랩/따라가기/인증 수 집계 캐시
- 만족도 평균 캐시
- Day 단위 일정
- 장소별 일정 아이템
- 코어 코스 여부
- 예산 포트폴리오 비율

### Travel Design / AI 설계

- `design_sessions`
- `design_session_style_tags`
- `design_itinerary_days`
- `design_itinerary_items`

포함 기능:

- AI 설계 세션
- 기존 ETF 따라가기 원본 참조
- 최종 저장된 ETF 참조
- 입력 예산, 여행 일수, 인원, 동행 유형
- 지역상생 옵션
- AI 원본 응답 JSON 보관
- 코어 코스 유지율
- 설계 중인 일정
- 원본 ETF 일정 아이템 참조

### Social Action

- `etf_likes`
- `etf_scraps`
- `etf_follows`
- `etf_ratings`

포함 기능:

- 좋아요
- 스크랩/저장
- 따라가기 기록
- 평점
- 사용자별 중복 액션 방지

### Verification / Reward

- `trip_executions`
- `card_payment_events`
- `trip_verifications`
- `reward_histories`
- `local_contributions`

포함 기능:

- ETF 기반 여행 시작/진행/완료 기록
- mock 또는 하나카드 결제 이벤트
- 카드 결제 기반 여행 인증
- 장소/지역/시간 매칭 결과
- 리워드 적립/차감 이력
- 지역 기여도 계산 결과

## 향후 개선 후보

아래 항목은 V1에서 빠진 문제가 아니라, 정책과 API가 구체화될 때 별도 마이그레이션으로 확장할 후보들이다.

### 1. 개발용 Seed 데이터 추가

필요 시점:

- 프론트/백엔드 API 연동을 시작하기 전
- Swagger나 프론트 화면에서 실제 목록 데이터를 확인해야 할 때

예상 마이그레이션:

```text
V2__seed_initial_dev_data.sql
```

포함 후보:

- 개발용 사용자 3명
- 충청권 지역 8~10개
- 지역별 장소 10~20개
- 스타일 태그
- 샘플 Travel ETF
- ETF 일정
- 좋아요/스크랩/따라가기/평점 데이터
- mock 카드 결제 내역
- 샘플 리워드 내역

주의:

- 운영 데이터와 혼동되지 않도록 개발용 seed임을 명확히 한다.
- 실제 운영 배포 전에는 dev seed 전략을 별도로 분리한다.

### 2. Refresh Token / Session 테이블

필요 시점:

- access token만으로 부족해 refresh token을 도입할 때
- 기기별 로그인 유지, 로그아웃, 토큰 회전이 필요할 때

테이블 후보:

- `refresh_tokens`
- `user_sessions`

검토 컬럼:

- `user_id`
- `token_hash`
- `device_id`
- `expires_at`
- `revoked_at`
- `created_at`

현재 판단:

- MVP에서 access token만 사용한다면 V1에 포함하지 않는 것이 적절하다.

### 3. AI Generation Log 테이블

필요 시점:

- AI 호출 이력을 추적해야 할 때
- 프롬프트 버전 관리가 필요할 때
- AI 호출 비용, latency, 실패 원인을 기록해야 할 때
- AI 결과 재생성/디버깅이 중요해질 때

테이블 후보:

- `ai_generation_logs`

검토 컬럼:

- `user_id`
- `design_session_id`
- `request_type`
- `prompt_version`
- `request_payload`
- `response_payload`
- `status`
- `error_message`
- `latency_ms`
- `created_at`

현재 판단:

- V1은 `design_sessions.ai_generated_raw_json`으로 AI 결과 원본을 보관한다.
- 별도 로그 테이블은 AI 운영 정책이 구체화된 뒤 추가한다.

### 4. ETF Ranking Snapshot 테이블

필요 시점:

- 급상승 ETF를 일/주간 기준으로 안정적으로 계산해야 할 때
- 과거 랭킹 이력을 보여줘야 할 때
- 실시간 count 계산이 부담될 때

테이블 후보:

- `etf_ranking_snapshots`

검토 컬럼:

- `travel_etf_id`
- `ranking_type`
- `rank`
- `score`
- `snapshot_date`
- `created_at`

현재 판단:

- MVP에서는 `travel_etfs`의 집계 캐시와 social action 테이블의 `created_at`으로 계산 가능하다.
- 랭킹 이력 화면 또는 batch job이 확정되면 추가한다.

### 5. Verification Matching 정책 보완

필요 시점:

- 카드 결제 인증 기준이 구체화될 때
- 거리, 시간, 카테고리, 금액 기준을 점수화해야 할 때

보완 후보:

- `trip_verifications`에 상세 점수 컬럼 추가
- 인증 매칭 로그 테이블 추가

컬럼 후보:

- `distance_score`
- `time_score`
- `category_score`
- `amount_score`
- `matched_distance_meters`

현재 판단:

- V1은 `matched_score`와 `reason`으로 단순 인증 결과를 저장한다.
- 정책이 안정화되면 상세 점수를 분리한다.

### 6. Reward 정책 보완

필요 시점:

- 월 단위 정산 정책이 확정될 때
- 리워드 지급 상태, 회수, 만료 정책이 필요할 때
- 포인트 이력과 정산 결과를 분리해야 할 때

테이블 후보:

- `reward_settlements`
- `reward_policies`

검토 컬럼:

- `settlement_month`
- `total_point`
- `status`
- `confirmed_at`
- `policy_version`

현재 판단:

- V1은 `reward_histories`로 포인트 증감 이력을 관리한다.
- 정산/만료 정책은 이후 마이그레이션으로 추가한다.

### 7. 외부 결제 연동 보안 보완

필요 시점:

- 실제 하나카드 API 연동을 시작할 때
- 외부 결제 ID, 원본 payload 저장 범위를 확정할 때

보완 후보:

- 민감정보 마스킹 정책 문서화
- `raw_payload` 저장 범위 제한
- 외부 API 응답 저장용 별도 audit 테이블 검토

현재 판단:

- V1은 `card_payment_events.raw_payload`를 제공하지만, 민감정보 저장은 금지하거나 마스킹해야 한다.

## 마이그레이션 작성 규칙

새로운 DB 변경은 기존 `V1`을 수정하지 않고 새 파일로 추가한다.

예시:

```text
V1__init_tripio_schema.sql
V2__seed_initial_dev_data.sql
V3__add_refresh_tokens.sql
V4__add_ai_generation_logs.sql
```

규칙:

- 이미 팀원이 적용한 마이그레이션 파일은 수정하지 않는다.
- 변경이 필요하면 새 버전 파일을 만든다.
- 테이블/컬럼 삭제는 영향 범위를 팀에서 확인한 뒤 진행한다.
- Java enum과 DB `CHECK` 제약의 값은 반드시 함께 검토한다.
- seed 데이터는 스키마 변경과 분리한다.

## GitHub Issue로 분리할 작업 후보

팀 공유 시 아래 제목으로 이슈를 만들 수 있다.

```text
[DB] 개발용 seed 데이터 추가
[DB] JWT refresh token/session 테이블 도입 검토
[DB] AI generation log 테이블 도입 검토
[DB] ETF ranking snapshot 테이블 도입 검토
[DB] 카드 인증 매칭 상세 점수 컬럼 검토
[DB] 월 단위 리워드 정산 테이블 검토
[DB] 하나카드 연동 raw payload 저장 정책 검토
```

## 현재 결론

V1은 TRIPIO 초기 개발을 시작하기 위한 기준 스키마로 사용한다.

향후 변경은 API 구현, 인증 정책, AI 운영 정책, 리워드 정책이 구체화되는 시점에 Flyway 마이그레이션으로 누적한다.
