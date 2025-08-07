# stock-batch


# 1. Docker 이미지 빌드
~~~
docker build -t stock-batch .
~~~

# 2. Docker 실행 예시 (prod 프로파일, Config 서버 사용자 지정)
~~~
docker run -d \
-p 8085:8085 \
-e SPRING_PROFILES_ACTIVE=dev \
-e CONFIG_SERVER_USER=아이디 \
-e CONFIG_SERVER_PASSWORD=비밀번호 \
--name stock-batch \
stock-batch
~~~
