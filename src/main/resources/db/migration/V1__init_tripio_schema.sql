-- TRIPIO ERD v1
-- 기준 DB: PostgreSQL
-- 목적: API 설계 전에 A/B/C 개발자가 공통으로 합의할 물리 ERD 초안
-- 작성 기준: backend-domain-functional-spec.md

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  travel_style VARCHAR(100),
  reward_point BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'WITHDRAWN'))
);

COMMENT ON TABLE users IS 'C 담당: 사용자 계정 기본 정보';
COMMENT ON COLUMN users.password_hash IS 'BCrypt 등 단방향 해시 저장. 원문 비밀번호 저장 금지';
COMMENT ON COLUMN users.reward_point IS '마이페이지 빠른 조회용 현재 보유 포인트 캐시';

CREATE TABLE regions (
  id BIGSERIAL PRIMARY KEY,
  parent_region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
  name VARCHAR(100) NOT NULL,
  region_type VARCHAR(30) NOT NULL,
  latitude NUMERIC(10, 7) NOT NULL,
  longitude NUMERIC(10, 7) NOT NULL,
  per_score INTEGER NOT NULL DEFAULT 0,
  local_contribution_base_score INTEGER NOT NULL DEFAULT 0,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_regions_type CHECK (region_type IN ('COUNTRY', 'PROVINCE', 'CITY', 'COUNTY')),
  CONSTRAINT ck_regions_per_score CHECK (per_score BETWEEN 0 AND 100),
  CONSTRAINT ck_regions_local_base_score CHECK (local_contribution_base_score BETWEEN 0 AND 100)
);

COMMENT ON TABLE regions IS 'C 담당: 지도, 지역 리포트, PER 기준 데이터';
COMMENT ON COLUMN regions.parent_region_id IS '충청남도 > 공주 같은 계층형 지역 표현';

CREATE INDEX idx_regions_parent ON regions(parent_region_id);
CREATE INDEX idx_regions_type ON regions(region_type);

CREATE TABLE user_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  profile_image_url TEXT,
  introduction TEXT,
  preferred_region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE user_profiles IS 'C 담당: 사용자 프로필 확장 정보';

CREATE TABLE places (
  id BIGSERIAL PRIMARY KEY,
  region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE RESTRICT,
  name VARCHAR(150) NOT NULL,
  address VARCHAR(255) NOT NULL,
  latitude NUMERIC(10, 7) NOT NULL,
  longitude NUMERIC(10, 7) NOT NULL,
  category VARCHAR(40) NOT NULL,
  is_local BOOLEAN NOT NULL DEFAULT false,
  is_traditional_market BOOLEAN NOT NULL DEFAULT false,
  is_core_spot BOOLEAN NOT NULL DEFAULT false,
  is_verifiable BOOLEAN NOT NULL DEFAULT true,
  estimated_cost INTEGER NOT NULL DEFAULT 0,
  description TEXT,
  image_url TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_places_category CHECK (
    category IN ('LODGING', 'RESTAURANT', 'CAFE', 'ACTIVITY', 'FESTIVAL', 'MARKET', 'TRANSPORT', 'ETC')
  ),
  CONSTRAINT ck_places_estimated_cost CHECK (estimated_cost >= 0)
);

COMMENT ON TABLE places IS 'A/B/C 공통 기준 데이터: ETF 일정, AI 설계, 카드 인증에서 모두 참조';
COMMENT ON COLUMN places.is_verifiable IS '카드 결제 인증 후보 장소로 사용할 수 있는지 여부';

CREATE INDEX idx_places_region ON places(region_id);
CREATE INDEX idx_places_category ON places(category);
CREATE INDEX idx_places_core ON places(region_id, is_core_spot);
CREATE INDEX idx_places_verifiable ON places(region_id, is_verifiable);

CREATE TABLE festivals (
  id BIGSERIAL PRIMARY KEY,
  region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
  name VARCHAR(150) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  description TEXT,
  location VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_festivals_date_range CHECK (end_date >= start_date)
);

COMMENT ON TABLE festivals IS 'C 담당: 지역 축제/행사 정보';

CREATE INDEX idx_festivals_region ON festivals(region_id);
CREATE INDEX idx_festivals_period ON festivals(start_date, end_date);

CREATE TABLE style_tags (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE style_tags IS 'A/B/C 공통: 힐링, 역사, 로컬푸드 같은 여행 스타일 태그';

CREATE TABLE user_preferred_style_tags (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  style_tag_id BIGINT NOT NULL REFERENCES style_tags(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, style_tag_id)
);

COMMENT ON TABLE user_preferred_style_tags IS 'C 담당: 사용자 선호 스타일 태그';

CREATE TABLE travel_etfs (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE RESTRICT,
  title VARCHAR(150) NOT NULL,
  summary TEXT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  total_budget INTEGER NOT NULL,
  duration_days INTEGER NOT NULL,
  local_contribution_score INTEGER NOT NULL DEFAULT 0,
  region_value_score INTEGER NOT NULL DEFAULT 0,
  expected_reward INTEGER NOT NULL DEFAULT 0,
  thumbnail_url TEXT,
  like_count INTEGER NOT NULL DEFAULT 0,
  scrap_count INTEGER NOT NULL DEFAULT 0,
  follow_count INTEGER NOT NULL DEFAULT 0,
  verification_count INTEGER NOT NULL DEFAULT 0,
  rating_average NUMERIC(3, 2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_travel_etfs_status CHECK (status IN ('DRAFT', 'PRIVATE', 'PUBLIC', 'ARCHIVED')),
  CONSTRAINT ck_travel_etfs_budget CHECK (total_budget >= 0),
  CONSTRAINT ck_travel_etfs_duration CHECK (duration_days > 0),
  CONSTRAINT ck_travel_etfs_local_score CHECK (local_contribution_score BETWEEN 0 AND 100),
  CONSTRAINT ck_travel_etfs_region_score CHECK (region_value_score BETWEEN 0 AND 100),
  CONSTRAINT ck_travel_etfs_expected_reward CHECK (expected_reward >= 0),
  CONSTRAINT ck_travel_etfs_counts CHECK (
    like_count >= 0 AND scrap_count >= 0 AND follow_count >= 0 AND verification_count >= 0
  ),
  CONSTRAINT ck_travel_etfs_rating_average CHECK (rating_average BETWEEN 0 AND 5)
);

COMMENT ON TABLE travel_etfs IS 'A 담당 중심: 여행 ETF 본문, 카드, 리포트의 기준 테이블';
COMMENT ON COLUMN travel_etfs.status IS 'DRAFT: 작성 중, PRIVATE: 비공개, PUBLIC: 공개, ARCHIVED: 보관';
COMMENT ON COLUMN travel_etfs.like_count IS '조회 성능을 위한 집계 캐시. etf_likes와 정합성 관리 필요';
COMMENT ON COLUMN travel_etfs.rating_average IS '조회 성능을 위한 평점 평균 캐시';

CREATE INDEX idx_travel_etfs_owner ON travel_etfs(owner_id);
CREATE INDEX idx_travel_etfs_region ON travel_etfs(region_id);
CREATE INDEX idx_travel_etfs_status ON travel_etfs(status);
CREATE INDEX idx_travel_etfs_public_region ON travel_etfs(status, region_id);
CREATE INDEX idx_travel_etfs_popular ON travel_etfs(like_count DESC, scrap_count DESC, follow_count DESC);

CREATE TABLE travel_etf_style_tags (
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  style_tag_id BIGINT NOT NULL REFERENCES style_tags(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (travel_etf_id, style_tag_id)
);

COMMENT ON TABLE travel_etf_style_tags IS 'A 담당: ETF 검색/필터용 스타일 태그 매핑';

CREATE INDEX idx_travel_etf_style_tags_tag ON travel_etf_style_tags(style_tag_id);

CREATE TABLE etf_itinerary_days (
  id BIGSERIAL PRIMARY KEY,
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  day_number INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_etf_itinerary_days_etf_day UNIQUE (travel_etf_id, day_number),
  CONSTRAINT ck_etf_itinerary_days_day CHECK (day_number > 0)
);

COMMENT ON TABLE etf_itinerary_days IS 'A 담당: 저장된 ETF의 Day 단위 일정';

CREATE TABLE etf_itinerary_items (
  id BIGSERIAL PRIMARY KEY,
  itinerary_day_id BIGINT NOT NULL REFERENCES etf_itinerary_days(id) ON DELETE CASCADE,
  place_id BIGINT NOT NULL REFERENCES places(id) ON DELETE RESTRICT,
  sequence INTEGER NOT NULL,
  start_time TIME,
  end_time TIME,
  estimated_cost INTEGER NOT NULL DEFAULT 0,
  is_core BOOLEAN NOT NULL DEFAULT false,
  memo TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_etf_itinerary_items_day_sequence UNIQUE (itinerary_day_id, sequence),
  CONSTRAINT ck_etf_itinerary_items_sequence CHECK (sequence > 0),
  CONSTRAINT ck_etf_itinerary_items_cost CHECK (estimated_cost >= 0),
  CONSTRAINT ck_etf_itinerary_items_time_range CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
);

COMMENT ON TABLE etf_itinerary_items IS 'A 담당: 저장된 ETF의 장소별 일정 아이템';
COMMENT ON COLUMN etf_itinerary_items.is_core IS '따라가기 시 유지율 계산 대상인 핵심 코스 여부';

CREATE INDEX idx_etf_itinerary_items_day ON etf_itinerary_items(itinerary_day_id);
CREATE INDEX idx_etf_itinerary_items_place ON etf_itinerary_items(place_id);

CREATE TABLE etf_portfolio_ratios (
  id BIGSERIAL PRIMARY KEY,
  travel_etf_id BIGINT NOT NULL UNIQUE REFERENCES travel_etfs(id) ON DELETE CASCADE,
  lodging_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  food_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  cafe_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  activity_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  festival_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  local_store_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  transport_ratio NUMERIC(5, 2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_etf_portfolio_ratios_non_negative CHECK (
    lodging_ratio >= 0 AND food_ratio >= 0 AND cafe_ratio >= 0 AND activity_ratio >= 0
    AND festival_ratio >= 0 AND local_store_ratio >= 0 AND transport_ratio >= 0
  )
);

COMMENT ON TABLE etf_portfolio_ratios IS 'A/B 담당: ETF 예산 포트폴리오 비율';
COMMENT ON COLUMN etf_portfolio_ratios.local_store_ratio IS '로컬 매장/전통시장 등 지역상생 소비 비율';

CREATE TABLE design_sessions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source_etf_id BIGINT REFERENCES travel_etfs(id) ON DELETE SET NULL,
  saved_etf_id BIGINT UNIQUE REFERENCES travel_etfs(id) ON DELETE SET NULL,
  region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE RESTRICT,
  status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
  input_budget INTEGER NOT NULL,
  input_days INTEGER NOT NULL,
  input_people_count INTEGER NOT NULL DEFAULT 1,
  input_companion_type VARCHAR(40),
  local_contribution_enabled BOOLEAN NOT NULL DEFAULT true,
  ai_generated_raw_json JSONB,
  core_retention_rate NUMERIC(5, 2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_design_sessions_status CHECK (
    status IN ('CREATED', 'AI_GENERATED', 'EDITING', 'SAVED_AS_ETF', 'CANCELLED')
  ),
  CONSTRAINT ck_design_sessions_budget CHECK (input_budget >= 0),
  CONSTRAINT ck_design_sessions_days CHECK (input_days > 0),
  CONSTRAINT ck_design_sessions_people CHECK (input_people_count > 0),
  CONSTRAINT ck_design_sessions_core_rate CHECK (core_retention_rate IS NULL OR core_retention_rate BETWEEN 0 AND 100)
);

COMMENT ON TABLE design_sessions IS 'B 담당: AI 생성, 따라가기, 일정 편집 중인 임시 여행 설계 세션';
COMMENT ON COLUMN design_sessions.source_etf_id IS '기존 ETF 따라가기인 경우 원본 ETF';
COMMENT ON COLUMN design_sessions.saved_etf_id IS '최종 저장 후 생성된 Travel ETF';
COMMENT ON COLUMN design_sessions.ai_generated_raw_json IS 'AI 원본 응답 보관. DTO 검증 후 저장';

CREATE INDEX idx_design_sessions_user ON design_sessions(user_id);
CREATE INDEX idx_design_sessions_source_etf ON design_sessions(source_etf_id);
CREATE INDEX idx_design_sessions_region ON design_sessions(region_id);
CREATE INDEX idx_design_sessions_status ON design_sessions(status);

CREATE TABLE design_session_style_tags (
  design_session_id BIGINT NOT NULL REFERENCES design_sessions(id) ON DELETE CASCADE,
  style_tag_id BIGINT NOT NULL REFERENCES style_tags(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (design_session_id, style_tag_id)
);

COMMENT ON TABLE design_session_style_tags IS 'B 담당: 설계 입력 조건의 스타일 태그';

CREATE TABLE design_itinerary_days (
  id BIGSERIAL PRIMARY KEY,
  design_session_id BIGINT NOT NULL REFERENCES design_sessions(id) ON DELETE CASCADE,
  day_number INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_design_itinerary_days_session_day UNIQUE (design_session_id, day_number),
  CONSTRAINT ck_design_itinerary_days_day CHECK (day_number > 0)
);

COMMENT ON TABLE design_itinerary_days IS 'B 담당: 설계 중인 Day 단위 일정';

CREATE TABLE design_itinerary_items (
  id BIGSERIAL PRIMARY KEY,
  design_itinerary_day_id BIGINT NOT NULL REFERENCES design_itinerary_days(id) ON DELETE CASCADE,
  place_id BIGINT NOT NULL REFERENCES places(id) ON DELETE RESTRICT,
  source_item_id BIGINT REFERENCES etf_itinerary_items(id) ON DELETE SET NULL,
  sequence INTEGER NOT NULL,
  start_time TIME,
  end_time TIME,
  estimated_cost INTEGER NOT NULL DEFAULT 0,
  is_core BOOLEAN NOT NULL DEFAULT false,
  memo TEXT,
  ai_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_design_itinerary_items_day_sequence UNIQUE (design_itinerary_day_id, sequence),
  CONSTRAINT ck_design_itinerary_items_sequence CHECK (sequence > 0),
  CONSTRAINT ck_design_itinerary_items_cost CHECK (estimated_cost >= 0),
  CONSTRAINT ck_design_itinerary_items_time_range CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
);

COMMENT ON TABLE design_itinerary_items IS 'B 담당: 설계 중인 장소별 일정 아이템';
COMMENT ON COLUMN design_itinerary_items.source_item_id IS '따라가기 원본 ETF 아이템. 코어 유지율 계산에 사용';

CREATE INDEX idx_design_itinerary_items_day ON design_itinerary_items(design_itinerary_day_id);
CREATE INDEX idx_design_itinerary_items_place ON design_itinerary_items(place_id);
CREATE INDEX idx_design_itinerary_items_source ON design_itinerary_items(source_item_id);

CREATE TABLE etf_likes (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_etf_likes_user_etf UNIQUE (user_id, travel_etf_id)
);

COMMENT ON TABLE etf_likes IS 'A 담당: ETF 좋아요';

CREATE INDEX idx_etf_likes_etf ON etf_likes(travel_etf_id);

CREATE TABLE etf_scraps (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_etf_scraps_user_etf UNIQUE (user_id, travel_etf_id)
);

COMMENT ON TABLE etf_scraps IS 'A 담당: 저장/스크랩한 ETF';

CREATE INDEX idx_etf_scraps_etf ON etf_scraps(travel_etf_id);

CREATE TABLE etf_follows (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  design_session_id BIGINT NOT NULL UNIQUE REFERENCES design_sessions(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE etf_follows IS 'A/B 담당: ETF 따라가기 기록. 따라가기는 DesignSession 생성 액션';

CREATE INDEX idx_etf_follows_user ON etf_follows(user_id);
CREATE INDEX idx_etf_follows_source_etf ON etf_follows(source_etf_id);

CREATE TABLE etf_ratings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE CASCADE,
  score INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_etf_ratings_user_etf UNIQUE (user_id, travel_etf_id),
  CONSTRAINT ck_etf_ratings_score CHECK (score BETWEEN 1 AND 5)
);

COMMENT ON TABLE etf_ratings IS 'A 담당: ETF 만족도/별점';

CREATE INDEX idx_etf_ratings_etf ON etf_ratings(travel_etf_id);

CREATE TABLE trip_executions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  travel_etf_id BIGINT NOT NULL REFERENCES travel_etfs(id) ON DELETE RESTRICT,
  status VARCHAR(40) NOT NULL DEFAULT 'READY',
  started_at TIMESTAMPTZ,
  ended_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_trip_executions_status CHECK (
    status IN ('READY', 'IN_PROGRESS', 'COMPLETED', 'PARTIALLY_VERIFIED', 'FAILED_VERIFICATION')
  ),
  CONSTRAINT ck_trip_executions_period CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
);

COMMENT ON TABLE trip_executions IS 'C 담당: 사용자가 ETF 기반으로 시작한 실제 여행 수행 기록';

CREATE INDEX idx_trip_executions_user ON trip_executions(user_id);
CREATE INDEX idx_trip_executions_etf ON trip_executions(travel_etf_id);
CREATE INDEX idx_trip_executions_status ON trip_executions(status);

CREATE TABLE card_payment_events (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  payment_provider VARCHAR(40) NOT NULL,
  merchant_name VARCHAR(150) NOT NULL,
  merchant_address VARCHAR(255),
  region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
  category VARCHAR(50) NOT NULL,
  amount INTEGER NOT NULL,
  paid_at TIMESTAMPTZ NOT NULL,
  latitude NUMERIC(10, 7),
  longitude NUMERIC(10, 7),
  external_payment_id VARCHAR(150),
  raw_payload JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_card_payment_events_provider CHECK (payment_provider IN ('MOCK', 'HANA_CARD')),
  CONSTRAINT ck_card_payment_events_category CHECK (
    category IN ('LODGING', 'RESTAURANT', 'CAFE', 'ACTIVITY', 'FESTIVAL', 'MARKET', 'TRANSPORT', 'TRADITIONAL_MARKET', 'LOCAL_STORE', 'ETC')
  ),
  CONSTRAINT ck_card_payment_events_amount CHECK (amount > 0),
  CONSTRAINT uq_card_payment_events_external UNIQUE (payment_provider, external_payment_id)
);

COMMENT ON TABLE card_payment_events IS 'C 담당: mock 또는 하나카드 결제 내역 이벤트';
COMMENT ON COLUMN card_payment_events.raw_payload IS '외부 결제 API 원본 응답 저장용. 민감정보는 저장 금지 또는 마스킹';

CREATE INDEX idx_card_payment_events_user_paid ON card_payment_events(user_id, paid_at DESC);
CREATE INDEX idx_card_payment_events_region ON card_payment_events(region_id);

CREATE TABLE trip_verifications (
  id BIGSERIAL PRIMARY KEY,
  trip_execution_id BIGINT NOT NULL REFERENCES trip_executions(id) ON DELETE CASCADE,
  payment_event_id BIGINT REFERENCES card_payment_events(id) ON DELETE SET NULL,
  place_id BIGINT REFERENCES places(id) ON DELETE SET NULL,
  verification_type VARCHAR(50) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  matched_score NUMERIC(5, 2) NOT NULL DEFAULT 0,
  reason TEXT,
  verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_trip_verifications_type CHECK (
    verification_type IN ('CARD_PAYMENT', 'LOCAL_STORE_PAYMENT', 'TRADITIONAL_MARKET_PAYMENT', 'REGION_PAYMENT')
  ),
  CONSTRAINT ck_trip_verifications_status CHECK (status IN ('PENDING', 'VERIFIED', 'PARTIALLY_VERIFIED', 'REJECTED')),
  CONSTRAINT ck_trip_verifications_score CHECK (matched_score BETWEEN 0 AND 100)
);

COMMENT ON TABLE trip_verifications IS 'C 담당: 카드 결제 이벤트와 ETF 일정/지역/장소 매칭 인증 결과';

CREATE INDEX idx_trip_verifications_trip ON trip_verifications(trip_execution_id);
CREATE INDEX idx_trip_verifications_payment ON trip_verifications(payment_event_id);
CREATE INDEX idx_trip_verifications_place ON trip_verifications(place_id);
CREATE INDEX idx_trip_verifications_status ON trip_verifications(status);

CREATE TABLE reward_histories (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  trip_execution_id BIGINT REFERENCES trip_executions(id) ON DELETE SET NULL,
  region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
  reward_type VARCHAR(50) NOT NULL,
  point INTEGER NOT NULL,
  reason TEXT NOT NULL,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_reward_histories_type CHECK (
    reward_type IN ('TRIP_COMPLETION', 'LOCAL_PAYMENT', 'TRADITIONAL_MARKET', 'CORE_COURSE', 'UNDERVALUED_REGION', 'ADJUSTMENT')
  )
);

COMMENT ON TABLE reward_histories IS 'C 담당: 사용자 포인트 증감 이력';
COMMENT ON COLUMN reward_histories.point IS '적립은 양수, 차감/정정은 음수 허용';

CREATE INDEX idx_reward_histories_user_calculated ON reward_histories(user_id, calculated_at DESC);
CREATE INDEX idx_reward_histories_trip ON reward_histories(trip_execution_id);
CREATE INDEX idx_reward_histories_region ON reward_histories(region_id);

CREATE TABLE local_contributions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  region_id BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
  trip_execution_id BIGINT REFERENCES trip_executions(id) ON DELETE SET NULL,
  contribution_score INTEGER NOT NULL DEFAULT 0,
  local_payment_amount INTEGER NOT NULL DEFAULT 0,
  traditional_market_payment_amount INTEGER NOT NULL DEFAULT 0,
  verified_place_count INTEGER NOT NULL DEFAULT 0,
  calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_local_contributions_score CHECK (contribution_score BETWEEN 0 AND 100),
  CONSTRAINT ck_local_contributions_amounts CHECK (
    local_payment_amount >= 0 AND traditional_market_payment_amount >= 0 AND verified_place_count >= 0
  )
);

COMMENT ON TABLE local_contributions IS 'C 담당: 지역 기여도 계산 결과';

CREATE INDEX idx_local_contributions_user ON local_contributions(user_id);
CREATE INDEX idx_local_contributions_region ON local_contributions(region_id);
CREATE INDEX idx_local_contributions_trip ON local_contributions(trip_execution_id);
