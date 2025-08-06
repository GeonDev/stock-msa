# 1단계: 빌드
FROM gradle:8.7.0-jdk17 AS builder

WORKDIR /app

COPY . .

# build without running tests
RUN gradle bootJar -x test

# 2단계: 실행
FROM eclipse-temurin:17-jdk-alpine

# 환경변수 기본값 설정 (필요시 오버라이딩)
ENV CONFIG_SERVER_USER=admin
ENV CONFIG_SERVER_PASSWORD=1234
ENV SPRING_PROFILES_ACTIVE=default

WORKDIR /app

# 빌드한 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
  -Dspring.cloud.config.username=$CONFIG_SERVER_USER \
  -Dspring.cloud.config.password=$CONFIG_SERVER_PASSWORD \
  -jar /app/app.jar"]
