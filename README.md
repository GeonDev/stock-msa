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

## 로컬 개발 환경에서 실행

### 사전 준비
- Java 17
- Docker (H2 데이터베이스 서버 실행용)

### 1. 애플리케이션 빌드
아래 명령어를 사용하여 프로젝트를 빌드합니다.
```
./gradlew build
```

### 2. 프로필별 실행 방법

#### `local` 프로필 (H2 데이터베이스 사용)

`local` 프로필은 별도의 설정 서버 없이 영구적인 H2 데이터베이스를 Docker 컨테이너로 실행하여 사용합니다.


**1) API 키 설정 (`.env` 파일)**

프로젝트 루트 경로에 `.env.example` 파일을 복사하여 `.env` 파일을 생성하고, 파일 내의 키 값을 실제 API 키로 채워주세요. 이 파일은 Git에서 추적되지 않습니다.

```
cp .env.example .env
```

이제 `.env` 파일을 열어 `YOUR_DATA_GO_SERVICE_KEY`와 `YOUR_DART_API_KEY` 값을 실제 키로 수정합니다.

**2) H2 Docker 컨테이너 실행**

터미널에서 아래 명령을 실행하여 H2 데이터베이스 서버를 시작합니다.

```
docker run -d --name h2-db-server \
  -p 8082:81 \
  -p 1521:1521 \
  -v h2-data:/opt/h2-data \
  oscarfonts/h2
```

- `8082` 포트는 H2 웹 콘솔용입니다 (`http://localhost:8082`)
- `1521` 포트는 애플리케이션 연결용 TCP 포트입니다.
- `h2-data` 볼륨에 데이터가 영구적으로 저장됩니다.

**3) 애플리케이션 실행**

```
java -jar -Dspring.profiles.active=local build/libs/stock-batch-0.0.1-SNAPSHOT.jar
```

#### `dev` 프로필 (MariaDB 사용)
`dev` 프로필은 Spring Cloud Config 서버와 MariaDB 데이터베이스가 필요합니다. 데이터베이스 연결 정보 등 주요 설정은 Config 서버로부터 받아옵니다.

**1) 사전 준비**
- 실행 중인 Spring Cloud Config 서버 (`http://localhost:9000`)
- 실행 중인 MariaDB 서버
- Config 서버에 `stock-batch` 애플리케이션의 `dev` 프로필 설정 (데이터베이스 URL, 자격 증명 등)

**2) 애플리케이션 실행**
`CONFIG_SERVER_USER`, `CONFIG_SERVER_PASSWORD`, `CONFIG_SERVER_URL` 환경 변수를 설정한 후 아래 명령어로 애플리케이션을 실행합니다.
```bash
java -jar -Dspring.profiles.active=dev build/libs/stock-batch-0.0.1-SNAPSHOT.jar
```
