package com.stock.configServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

	public static void main(String[] args) {

		String encrypt = System.getenv().getOrDefault("CONFIG_ENCRYPT_KEY", "secret");
		System.setProperty("encrypt.key", encrypt);

		try {
			String key = Files.readString(Path.of("/data/keys/private-key"));
			System.setProperty("spring.cloud.config.server.git.private-key", key);
		} catch (IOException e) {
			throw new RuntimeException("SSH 키 로드 실패", e);
		}

		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
