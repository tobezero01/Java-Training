package com.ducnhu.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ducnhu.catalog",
        "com.ducnhu.common","com.ducnhu.common.kafka",
        "com.ducnhu.common.redis"})
public class CatalogServiceApplication {
    public static void main(String[] args) {

        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
