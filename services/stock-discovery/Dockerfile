FROM amazoncorretto:17-alpine

WORKDIR /app

# 빌드된 JAR 파일을 컨테이너로 복사
COPY build/libs/*-SNAPSHOT.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]