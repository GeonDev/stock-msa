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

***주의: `application.yaml` 파일에 여러 프로파일 설정을 함께 정의할 경우, Gemini 분석 시 오류가 발생할 수 있습니다. 각 프로파일은 `application-{profile}.yaml`과 같이 별도의 파일로 분리하여 관리하는 것을 권장합니다.***

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

터미널에서 아래 명령을 실행하여 H2 데이터베이스 서버를 시작합니다. (Apple Silicon Mac 사용자는 `--platform linux/amd64` 옵션이 필요할 수 있습니다.)

```bash
docker run -d --name h2-db-server \
  --platform linux/amd64 \
  -p 1521:1521 \
  -v h2-data:/opt/h2-data \
  oscarfonts/h2
```

- `1521` 포트는 애플리케이션 연결용 TCP 포트입니다.
- `h2-data` 볼륨에 데이터가 영구적으로 저장됩니다.

**3) H2 데이터베이스 초기 설정 (최초 1회)**

H2 TCP 서버는 보안상 존재하지 않는 데이터베이스를 원격에서 자동으로 생성하지 않을 수 있습니다. 애플리케이션 실행 전, 아래 명령어를 터미널에 입력하여 컨테이너 내부에서 데이터베이스 파일을 직접 생성해 주세요.

```bash
# meta-db 생성
docker exec h2-db-server java -cp '/opt/h2/bin/*' org.h2.tools.RunScript -script /dev/null -url "jdbc:h2:file:/opt/h2-data/meta-db;MODE=MariaDB;CASE_INSENSITIVE_IDENTIFIERS=TRUE" -user sa -password ""

# data-db 생성
docker exec h2-db-server java -cp '/opt/h2/bin/*' org.h2.tools.RunScript -script /dev/null -url "jdbc:h2:file:/opt/h2-data/data-db;MODE=MariaDB;CASE_INSENSITIVE_IDENTIFIERS=TRUE" -user sa -password ""

```

*참고: `MODE=MariaDB` 설정은 로컬 H2 환경과 운영 MariaDB 환경 간의 SQL 호환성을 위해 추가되었습니다.*

이제 데이터베이스 파일(`/opt/h2-data/meta-db.mv.db` 등)이 생성되었으므로, 애플리케이션에서 TCP로 접속이 가능합니다.

**4) 애플리케이션 실행**

```bash
java -jar -Dspring.profiles.active=local build/libs/stock-batch-0.0.1-SNAPSHOT.jar
```

애플리케이션이 실행되면:
- **Batch 메타 테이블:** `spring.batch.jdbc.initialize-schema: always` 설정에 의해 `meta-db`에 자동 생성됩니다.
- **비즈니스 테이블:** JPA의 `hibernate.hbm2ddl.auto: update` 설정에 의해 `data-db`에 자동 생성됩니다.
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
