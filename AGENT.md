# TRIPIO Backend Agent Guide
## Communication

- 모든 답변, 요약, 구현 계획, 리뷰 코멘트는 기본적으로 한국어로 작성한다.
- 코드, 클래스명, 함수명, 커밋 메시지, API 경로, 에러 코드 등은 기존 프로젝트 컨벤션에 맞게 영어를 유지한다.
- 사용자가 명시적으로 영어 답변을 요청한 경우에만 영어로 답변한다.

## Product Context

TRIPIO is a travel-finance service for the Hana Youth Financial Talent Project.
The current MVP concept is "Travel ETF": users discover, save, like, and follow travel ETF reports, then create their own travel plan through AI design or follow existing ETFs.

Core screens:
- Home / Discovery: popular ETF, rising ETF, undervalued ETF, ETF search/filter/recommendation.
- Map: Korea > province > city/county, heatmap, regional report, top ETF list.
- Travel Design: create a new ETF or follow an existing ETF.
- My Page: created ETFs, saved ETFs, dividend/reward, local contribution.

## Backend Tech Stack

- Java 21
- Spring Boot 3.5
- Gradle
- PostgreSQL 16
- Spring Data JPA
- Flyway
- Spring Security
- springdoc-openapi
- Testcontainers

## My Scope: Developer A

Developer A owns:
- ETF report APIs
- ETF discovery/search/filter/ranking APIs
- Social actions around ETF: like, scrap/save, follow, rating

Main packages:
- `com.tripio.etf`
- `com.tripio.discovery`
- `com.tripio.social`

Main tables:
- `travel_etfs`
- `travel_etf_style_tags`
- `etf_itinerary_days`
- `etf_itinerary_items`
- `etf_portfolio_ratios`
- `etf_likes`
- `etf_scraps`
- `etf_follows`
- `etf_ratings`

## Architecture Rules

- Use domain-based packages.
- Do not create a new global response wrapper.
- All controller responses must use `ApiResponse<T>`.
- Do not return JPA entities directly from controllers.
- Throw domain exceptions and let `GlobalExceptionHandler` convert them.
- DB schema is managed by Flyway.
- Keep Hibernate `ddl-auto: validate`.
- Do not modify `V1__init_tripio_schema.sql` after shared agreement; create `V2__...sql` for schema changes.
- Match enum values with DB CHECK constraints.
- Keep JPA relationships minimal. Do not make bidirectional relationships unless required.
- Be careful with cached counts in `travel_etfs`: `like_count`, `scrap_count`, `follow_count`, `verification_count`, `rating_average`.

## Developer A API Priorities

Implement in small PRs:

1. ETF report detail
   - `GET /api/etfs/{etfId}`
   - Include title, region, theme/style tags, budget, duration, scores, thumbnail, itinerary days/items, portfolio ratios, user reaction counts.

2. ETF discovery list
   - `GET /api/etfs`
   - Support query, region, style tag filters, budget range, duration, sort.
   - Sort examples: popular, rising, undervalued, latest.

3. Home discovery
   - `GET /api/discovery/home`
   - Return popular ETFs, rising ETFs, undervalued ETFs.

4. Social actions
   - `POST /api/etfs/{etfId}/likes`
   - `DELETE /api/etfs/{etfId}/likes`
   - `POST /api/etfs/{etfId}/scraps`
   - `DELETE /api/etfs/{etfId}/scraps`

5. Follow ETF
   - `POST /api/etfs/{etfId}/follow`
   - Following an ETF should create a design session based on the source ETF.
   - Do not mutate the source ETF.
   - If the user changes core itinerary items, it should be treated as rebalancing, and core retention should be tracked.

## Testing Rules

Before finishing a backend task, run:
- `./gradlew test`

If Docker/Testcontainers is unavailable, at least run:
- `./gradlew testClasses`

Add tests for:
- Repository query behavior
- Service logic
- Controller response format

## Review Guidelines

- Check that no sensitive personal information is logged.
- Check that API responses follow `ApiResponse<T>`.
- Check that social actions are idempotent where possible.
- Check count cache consistency when likes/scraps/follows/ratings are created or removed.
- Check that public/private ETF visibility is respected.