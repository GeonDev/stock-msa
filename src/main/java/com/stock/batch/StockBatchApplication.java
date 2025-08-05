package com.stock.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StockBatchApplication {

	public static void main(String[] args) {

		// Config Server 인증 정보
		String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
		String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");

		System.setProperty("spring.cloud.config.username",username);
		System.setProperty("spring.cloud.config.password",password);

		// Eureka URL
		String eurekaUrl = String.format("http://%s:%s@localhost:9000/eureka", username, password);

		System.setProperty("eureka.client.service-url.defaultZone", eurekaUrl);

		SpringApplication.run(StockBatchApplication.class, args);
	}

}
