package com.stock.price;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.stock")
public class StockPriceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockPriceApplication.class, args);
    }
}