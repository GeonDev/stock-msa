package com.stock.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.stock")
public class StockStockApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockStockApplication.class, args);
    }
}