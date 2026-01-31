# 1) 빌드 스테이지
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || return 0

COPY . .
RUN gradle clean bootJar --no-daemon

# 2) 실행 스테이지
FROM eclipse-temurin:17-jdk

ENV CONFIG_SERVER_USER=admin
ENV CONFIG_SERVER_PASSWORD=1234
ENV CONFIG_ENCRYPT_KEY=keys

WORKDIR /app

# /app/keys 경로 사용
RUN mkdir -p /app/keys && \
    touch /app/keys/private-key

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]