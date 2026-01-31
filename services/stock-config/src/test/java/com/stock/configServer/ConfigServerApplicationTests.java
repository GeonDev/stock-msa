package com.stock.configServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private String baseUrl(String path) {
		return "http://localhost:" + port + path;
	}

	@BeforeAll
	static void setupPrivateKey() {
		try {
			String key = Files.readString(Path.of("/app/keys/private-key"));
			System.setProperty("spring.cloud.config.server.git.private-key", key);
		} catch (IOException e) {
			throw new RuntimeException("테스트에서 SSH 키 로드 실패", e);
		}
	}

	@Test
	void contextLoads() {
		String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
		String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");

		ResponseEntity<String> response = restTemplate
				.withBasicAuth(username, password)
				.getForEntity(baseUrl("/test/default"), String.class);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}

}
