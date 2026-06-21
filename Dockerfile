FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=builder /workspace/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
