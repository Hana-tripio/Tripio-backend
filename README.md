# Tripio-backend

TRIPIO 백엔드 서버입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5
- Gradle
- PostgreSQL 16
- Spring Data JPA
- Flyway
- Spring Security
- springdoc-openapi
- Testcontainers

## 로컬 DB 실행

백엔드는 로컬 개발 DB로 PostgreSQL을 사용합니다.

```bash
docker compose up -d
```

Docker Compose는 다음 DB를 생성합니다.

```text
host: localhost
port: 5432
database: tripio
username: tripio
password: tripio
```

DB를 종료하려면 다음 명령을 사용합니다.

```bash
docker compose down
```

데이터 볼륨까지 삭제하고 처음부터 다시 만들려면 다음 명령을 사용합니다.

```bash
docker compose down -v
```

## DB 마이그레이션

DB 스키마는 Flyway로 관리합니다.

마이그레이션 파일 위치:

```text
src/main/resources/db/migration
```

초기 스키마:

```text
V1__init_tripio_schema.sql
```

애플리케이션을 실행하면 Flyway가 아직 적용되지 않은 마이그레이션을 자동으로 실행합니다.

```bash
./gradlew bootRun
```

JPA 설정은 `ddl-auto: validate`를 사용합니다. 즉, Hibernate가 테이블을 자동 생성하지 않고, Flyway가 만든 DB 구조와 JPA 엔티티가 맞는지만 검증합니다.

ERD 원본 문서와 이미지는 아래 위치에서 관리합니다.

```text
docs/db/erd
```

## 테스트 실행

통합 테스트는 Testcontainers PostgreSQL을 사용합니다. 따라서 테스트를 실행하려면 Docker가 먼저 실행 중이어야 합니다.

```bash
./gradlew test
```

테스트는 로컬 `localhost:5432` DB에 의존하지 않습니다. 각 테스트 실행 시 Testcontainers가 PostgreSQL 컨테이너를 띄우고, Flyway 마이그레이션을 적용한 뒤 검증합니다.

## API 문서

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 초기 개발 주의사항

- 스키마 변경은 Hibernate `ddl-auto`가 아니라 Flyway 마이그레이션 파일로 관리합니다.
- 새 테이블이나 컬럼이 필요하면 `V2__...sql`, `V3__...sql`처럼 새 파일을 추가합니다.
- 개발용 seed 데이터는 스키마 파일과 분리해서 별도 마이그레이션으로 관리합니다.
- 현재 보안 설정은 초기 개발용이며, 실제 API 보호를 위해 JWT 필터와 인가 정책을 추가해야 합니다.

## 패키지 구조

도메인별 패키지 구조와 Entity 작성 원칙은 아래 문서에 정리되어 있습니다.

```text
docs/architecture/package-structure.md
```

공통 API 응답 형식과 도메인별 성공/에러 코드 작성 규칙은 아래 문서에 정리되어 있습니다.

```text
docs/architecture/api-response-format.md
```
