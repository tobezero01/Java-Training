package com.ducnhu.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.ducnhu.payment",
		"com.ducnhu.common",
		"com.ducnhu.common.dto",
		"com.ducnhu.common.helper",
		"com.ducnhu.common.kafka",
		"com.ducnhu.common.redis",
})
@EnableScheduling
@EnableFeignClients(basePackages = "com.ducnhu.payment")
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
