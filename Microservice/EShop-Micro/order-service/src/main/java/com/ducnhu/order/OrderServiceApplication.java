package com.ducnhu.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.ducnhu.order",
		"com.ducnhu.common",
		"com.ducnhu.common.kafka",
		"com.ducnhu.common.redis",
})
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
