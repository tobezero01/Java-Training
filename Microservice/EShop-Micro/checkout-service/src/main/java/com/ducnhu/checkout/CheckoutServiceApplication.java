package com.ducnhu.checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.ducnhu.common.kafka",   // để thấy RequestReplyClient
		"com.ducnhu.common.cache",
		"com.ducnhu.checkout",
		"com.ducnhu.common"// nếu cart có dùng RedisCacheService
})
@EnableFeignClients(basePackages = "com.ducnhu.checkout")
@EnableScheduling
public class CheckoutServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckoutServiceApplication.class, args);
	}

}
