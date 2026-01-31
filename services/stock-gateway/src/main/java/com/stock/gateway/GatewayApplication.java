package com.stock.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {

		// eureka Server 인증 정보
		String username = System.getenv().getOrDefault("EUREKA_SERVER_USER", "admin");
		String password = System.getenv().getOrDefault("EUREKA_SERVER_PASSWORD", "1234");

		// Eureka URL
		String eurekaUrl = String.format("http://%s:%s@localhost:8761/eureka", username, password);
		System.setProperty("eureka.client.service-url.defaultZone", eurekaUrl);

		SpringApplication.run(GatewayApplication.class, args);
	}

}
