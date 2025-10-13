package com.example.demo.process;

import com.example.demo.entity.Customer;
import com.example.demo.service.RedisCacheService;
import org.springframework.http.converter.json.GsonBuilderUtils;

public class ProductCacheProcess implements Runnable{
    private RedisCacheService redisCacheService;
    private final String CACHE_KEY = "product_cache_key";

    public ProductCacheProcess (RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (redisCacheService.checkExistsKey(CACHE_KEY)) {
                    Customer customer = (Customer)redisCacheService.rPop(CACHE_KEY);
                    redisCacheService.setValue("customer_"+customer.getId(), customer);
                } else {
                    Thread.sleep(100);
                }
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
