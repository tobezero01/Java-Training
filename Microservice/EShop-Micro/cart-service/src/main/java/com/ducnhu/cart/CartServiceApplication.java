package com.ducnhu.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.ducnhu.cart",
        "com.ducnhu.common",
        "com.ducnhu.common.kafka",
        "com.ducnhu.common.redis",
})
@EnableFeignClients(basePackages = "com.ducnhu.cart")
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }

}
