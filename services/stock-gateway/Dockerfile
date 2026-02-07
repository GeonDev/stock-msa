# Build stage
FROM gradle:8.5-jdk21 AS builder

WORKDIR /workspace

# Copy project files
COPY settings.gradle .
COPY build.gradle .
COPY modules ./modules
COPY services/stock-gateway ./services/stock-gateway

# Build the service
RUN gradle :services:stock-gateway:bootJar --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine

WORKDIR /app

COPY --from=builder /workspace/services/stock-gateway/build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
