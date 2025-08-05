package com.stock.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockBatchApplication {

	public static void main(String[] args) {

		String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
		String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");

		System.setProperty("spring.cloud.config.username",username);
		System.setProperty("spring.cloud.config.password",password);

		SpringApplication.run(StockBatchApplication.class, args);
	}

}
