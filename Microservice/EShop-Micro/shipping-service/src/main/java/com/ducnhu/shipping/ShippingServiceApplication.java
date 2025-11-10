package com.ducnhu.shipping;

import com.ducnhu.common.kafka.KafkaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {
		"com.ducnhu.shipping",
		"com.ducnhu.common",
		"com.ducnhu.common.kafka",
		"com.ducnhu.common.redis",
})@Import({com.ducnhu.common.kafka.KafkaConfig.class, KafkaConfig.class})
public class ShippingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShippingServiceApplication.class, args);
	}

}
