# config-server

Spring cloud config server  

도커 이미지 빌드
~~~
docker build -t config-server .
~~~

컨테이너 실행시 github id_rsa 파일 마운트 필요

~~~
docker run -d \
  -p 9000:9000 \
  -e CONFIG_SERVER_USER=아이디 \
  -e CONFIG_SERVER_PASSWORD=비밀번호 \
  -e CONFIG_ENCRYPT_KEY=비밀키 \
  -v /app/keys/private-key:/app/keys/private-key:ro \
  --name config-server \
  config-server
~~~