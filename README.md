# stock-batch


# 1. Docker 이미지 빌드
~~~
docker build -t stock-batch .
~~~

# 2. Docker 실행 예시 (prod 프로파일, Config 서버 사용자 지정)
~~~
docker run -d \
-p 8085:8085 \
-e SPRING_PROFILES_ACTIVE=local \
-e CONFIG_SERVER_USER=아이디 \
-e CONFIG_SERVER_PASSWORD=비밀번호 \
--name stock-batch \
stock-batch
~~~

## 로컬 개발 환경에서 실행

***주의: `application.yaml` 파일에 여러 프로파일 설정을 함께 정의할 경우, Gemini 분석 시 오류가 발생할 수 있습니다. 각 프로파일은 `application-{profile}.yaml`과 같이 별도의 파일로 분리하여 관리하는 것을 권장합니다.***

### 사전 준비
- Java 17
- Docker (MySQL 데이터베이스 서버 실행용)

### 1. 애플리케이션 빌드
아래 명령어를 사용하여 프로젝트를 빌드합니다.
```
./gradlew build
```

### 2. 프로필별 실행 방법

#### `local` 프로필 (MySQL 데이터베이스 사용)

`local` 프로필은 별도의 설정 서버 없이 영구적인 MySQL 데이터베이스를 Docker 컨테이너로 실행하여 사용합니다.


**1) API 키 설정 (`.env` 파일)**

프로젝트 루트 경로에 `.env.example` 파일을 복사하여 `.env` 파일을 생성하고, 파일 내의 키 값을 실제 API 키로 채워주세요. 이 파일은 Git에서 추적되지 않습니다.

```
cp .env.example .env
```

이제 `.env` 파일을 열어 `YOUR_DATA_GO_SERVICE_KEY`와 `YOUR_DART_API_KEY` 값을 실제 키로 수정합니다.

**2) MySQL Docker 컨테이너 실행**

터미널에서 아래 명령을 실행하여 MySQL 데이터베이스 서버를 시작합니다. (Apple Silicon Mac 사용자는 `--platform linux/amd64` 옵션이 필요할 수 있습니다.)

```bash
docker run -d --name mysql-db-server \
  --platform linux/amd64 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root1234 \
  mysql:8.0
```

- `3306` 포트는 애플리케이션 연결용 TCP 포트입니다.
- `MYSQL_ROOT_PASSWORD`는 `root1234`로 설정됩니다 (`application-local.yaml` 기본값).

**3) MySQL 데이터베이스 초기 설정 (최초 1회)**

애플리케이션 실행 전, 아래 명령어를 터미널에 입력하여 `meta_db`와 `data_db`를 생성해 주세요.

```bash
docker exec mysql-db-server mysql -uroot -proot1234 -e "CREATE DATABASE IF NOT EXISTS meta_db; CREATE DATABASE IF NOT EXISTS data_db;"
```

**4) 애플리케이션 실행**

```bash
java -jar -Dspring.profiles.active=local build/libs/stock-batch-0.0.1-SNAPSHOT.jar
```

애플리케이션이 실행되면:
- **Batch 메타 테이블:** `spring.batch.jdbc.initialize-schema: always` 설정에 의해 `meta_db`에 자동 생성됩니다.
- **비즈니스 테이블:** JPA의 `hibernate.hbm2ddl.auto: update` 설정에 의해 `data_db`에 자동 생성됩니다.
- **데이터 마이그레이션:** `src/main/resources/db/migration`에 정의된 Flyway 스크립트가 실행됩니다.

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
