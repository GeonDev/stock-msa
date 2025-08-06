# stock-batch


# 1. Docker 이미지 빌드
~~~
docker build -t stock-batch .
~~~

# 2. Docker 실행 예시 (prod 프로파일, Config 서버 사용자 지정)
~~~
docker run -e SPRING_PROFILES_ACTIVE=prod \
-e CONFIG_SERVER_USER=admin \
-e CONFIG_SERVER_PASSWORD=1234 \
-p 8085:8085 \
stock-batch
~~~
