package com.stock.batch;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient
public class StockBatchApplication {

	public static void main(String[] args) {

		// 현재 활성화된 프로파일 확인
		String activeProfile = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "local"));

		if ("local".equalsIgnoreCase(activeProfile)) {
			// .env 파일 로드
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		}
		// 'local' 프로파일이 아닐 경우에만 Config Server 및 Eureka 관련 설정 적용
		else {
			// Config Server 인증 정보
			String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
			String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");

			System.setProperty("spring.cloud.config.username", username);
			System.setProperty("spring.cloud.config.password", password);

			// configServer URL
			String configServerUrl = System.getenv().getOrDefault("CONFIG_SERVER_URL", "http://localhost:9000");
			System.setProperty("spring.config.import", String.format("optional:configserver:%s", configServerUrl));
		}


		SpringApplication.run(StockBatchApplication.class, args);
	}

}
