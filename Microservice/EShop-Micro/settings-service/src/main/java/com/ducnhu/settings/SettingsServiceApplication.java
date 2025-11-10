package com.ducnhu.settings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {
		"com.ducnhu.settings",
		"com.ducnhu.common",
		"com.ducnhu.common.kafka",
		"com.ducnhu.common.redis",
})
@ConfigurationPropertiesScan("com.ducnhu.common.security")
public class SettingsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SettingsServiceApplication.class, args);
	}

}
