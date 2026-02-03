package com.stock.corp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.stock")
public class StockCorpApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockCorpApplication.class, args);
    }
}