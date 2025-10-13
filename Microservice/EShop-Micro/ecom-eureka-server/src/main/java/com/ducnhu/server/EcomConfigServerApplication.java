package com.ducnhu.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EcomConfigServerApplication {

	public static void main(String[] args) {

		SpringApplication.run(EcomConfigServerApplication.class, args);
	}

}
