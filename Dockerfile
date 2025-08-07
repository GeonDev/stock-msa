# 1단계: 빌드
FROM gradle:8.7.0-jdk17 AS builder

WORKDIR /app
COPY . .
RUN gradle bootJar -x test

# 2단계: 실행
FROM eclipse-temurin:17-jdk

ENV CONFIG_SERVER_USER=admin
ENV CONFIG_SERVER_PASSWORD=1234
ENV SPRING_PROFILES_ACTIVE=default

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
  -jar /app/app.jar"]
