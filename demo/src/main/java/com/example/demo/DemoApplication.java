package com.example.demo;

import com.example.demo.entity.Department;
import com.example.demo.process.BusinessProcess;
import com.example.demo.process.ProductCacheProcess;
import com.example.demo.process.ProductRelatedProcess;
import com.example.demo.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class DemoApplication {

    @Autowired
    private RedisCacheService redisCacheService;
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean("depart1")
    public Department getInstance() {
        return new Department(1, "department1");
    }

    @Bean("depart2")
    public Department getInstance2() {
        return new Department(2, "department2");
    }



    @Bean
    public Integer init() {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        // cache product
        for (int i=0; i<3; i++) {
            ProductCacheProcess process =  new ProductCacheProcess(redisCacheService);
            executorService.execute(process);
        }

        for (int i=0; i<3; i++) {
            ProductRelatedProcess process =  new ProductRelatedProcess(redisCacheService);
            executorService.execute(process);
        }
        return 1;
    }

}
