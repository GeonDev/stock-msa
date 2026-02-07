# Build stage
FROM gradle:8.5-jdk21 AS builder

WORKDIR /workspace

# Copy project files
COPY settings.gradle .
COPY build.gradle .
COPY modules ./modules
COPY services/stock-finance ./services/stock-finance

# Build the service
RUN gradle :services:stock-finance:bootJar --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine

WORKDIR /app

COPY --from=builder /workspace/services/stock-finance/build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
