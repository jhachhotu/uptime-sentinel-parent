package com.sentinel.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
        System.setProperty("eureka.instance.hostname", "localhost");
        System.setProperty("eureka.instance.prefer-ip-address", "true");

        SpringApplication.run(ConfigServerApplication.class, args);
    }

}
