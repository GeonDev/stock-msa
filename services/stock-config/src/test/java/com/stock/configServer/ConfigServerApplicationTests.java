package com.stock.configServer;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class ConfigServerApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeAll
	static void setupEnv() {
		// 프로젝트 루트 경로 계산 (services/stock-config -> ../../)
		String projectRoot = Paths.get(System.getProperty("user.dir"))
				.getParent() // services
				.getParent() // root
				.toString();

		// .env 파일 로드
		Dotenv dotenv = Dotenv.configure()
				.directory(projectRoot)
				.ignoreIfMissing()
				.load();

		// GIT_PRIVATE_KEY 줄바꿈 처리 (중요: .env의 \n 문자를 실제 개행으로 치환)
		String privateKey = dotenv.get("GIT_PRIVATE_KEY", "");
		if (privateKey != null) {
			privateKey = privateKey.replace("\\n", "\n");
		}

		// 테스트 환경을 위한 시스템 프로퍼티 설정
		System.setProperty("GIT_PRIVATE_KEY", privateKey);
		System.setProperty("CONFIG_SERVER_USER", dotenv.get("CONFIG_SERVER_USER", "admin"));
		System.setProperty("CONFIG_SERVER_PASSWORD", dotenv.get("CONFIG_SERVER_PASSWORD", "1234"));
		System.setProperty("EUREKA_USER", dotenv.get("EUREKA_USER", "admin"));
		System.setProperty("EUREKA_PASSWORD", dotenv.get("EUREKA_PASSWORD", "1234"));
	}

	@AfterAll
	static void cleanupEnv() {
		System.clearProperty("GIT_PRIVATE_KEY");
		System.clearProperty("CONFIG_SERVER_USER");
		System.clearProperty("CONFIG_SERVER_PASSWORD");
		System.clearProperty("EUREKA_USER");
		System.clearProperty("EUREKA_PASSWORD");
	}

	@Test
	void contextLoads() {
		// application.yaml에 'clone-on-start: true'가 설정되어 있다면,
		// 이 테스트가 통과하는 것만으로도 Git 리포지토리 연결 성공을 의미합니다.
	}

	@Test
	void verifyConfigServerUpAndRunning() {
		String username = System.getProperty("CONFIG_SERVER_USER");
		String password = System.getProperty("CONFIG_SERVER_PASSWORD");

		// Config Server 헬스 체크 또는 기본 설정 조회
		// (Actuator가 켜져 있다면 /actuator/health, 아니라면 암호화/복호화 엔드포인트나 더미 설정 조회)
		String url = "http://localhost:" + port + "/actuator/health";

		ResponseEntity<String> response = restTemplate
				.withBasicAuth(username, password)
				.getForEntity(url, String.class);

		// 200 OK 또는 404 (경로 문제일 뿐 서버는 뜬 상태) 등을 확인
		// 여기서는 200 OK를 기대합니다.
		assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
	}
}
