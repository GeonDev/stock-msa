package com.stock.stock;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.stock")
public class StockStockApplication {
    public static void main(String[] args) {
        String activeProfile = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "local"));

        if ("local".equalsIgnoreCase(activeProfile)) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } else {
            String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
            String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");
            System.setProperty("spring.cloud.config.username", username);
            System.setProperty("spring.cloud.config.password", password);

            String configServerUrl = System.getenv().getOrDefault("CONFIG_SERVER_URL", "http://localhost:9000");
            System.setProperty("spring.config.import", String.format("optional:configserver:%s", configServerUrl));
        }

        SpringApplication.run(StockStockApplication.class, args);
    }
}