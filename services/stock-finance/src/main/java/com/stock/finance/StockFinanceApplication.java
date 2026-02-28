package com.stock.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.stock")
@EnableCaching
@org.springframework.scheduling.annotation.EnableAsync
public class StockFinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockFinanceApplication.class, args);
    }
}