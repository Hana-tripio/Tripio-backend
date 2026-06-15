# TRIPIO Backend DB Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 백엔드가 PostgreSQL/Flyway 기반으로 TRIPIO ERD 스키마를 생성하고, 팀원이 같은 방식으로 테스트할 수 있게 만든다.

**Architecture:** Flyway `V1` 마이그레이션에 ERD DDL을 배치하고 Spring Boot가 애플리케이션 시작 시 스키마를 적용한다. 테스트는 Testcontainers PostgreSQL을 사용해 로컬 DB 상태와 분리한다.

**Tech Stack:** Spring Boot 3.5, Java 21, Gradle, PostgreSQL 16, Flyway, Testcontainers, JUnit 5.

---

### Task 1: Testcontainers 기반 테스트 DB 추가

**Files:**
- Modify: `build.gradle`
- Create: `src/test/java/com/tripio/support/IntegrationTestSupport.java`
- Modify: existing `@SpringBootTest` tests to extend the support class

- [ ] Add Testcontainers PostgreSQL dependencies.
- [ ] Add shared integration test support with PostgreSQL container and dynamic datasource properties.
- [ ] Run tests and confirm the previous local-role failure is replaced by migration-related behavior.

### Task 2: Flyway 스키마 마이그레이션 추가

**Files:**
- Create: `src/main/resources/db/migration/V1__init_tripio_schema.sql`
- Modify: `src/main/resources/application.yml`

- [ ] Copy the TRIPIO PostgreSQL ERD DDL into Flyway `V1`.
- [ ] Enable Flyway in application configuration.
- [ ] Keep `spring.jpa.hibernate.ddl-auto=validate` so Hibernate never creates schema implicitly.

### Task 3: 실행 문서 보강

**Files:**
- Modify: `README.md`

- [ ] Document Docker PostgreSQL startup.
- [ ] Document Flyway migration behavior.
- [ ] Document test execution requirements.

### Task 4: Verification

**Commands:**
- `./gradlew test`
- If Docker is unavailable, record the exact blocker and command output.

