package com.example.demo.process;

import com.example.demo.entity.Customer;
import com.example.demo.service.RedisCacheService;

import java.util.Arrays;
import java.util.List;

public class ProductRelatedProcess implements Runnable{

    private RedisCacheService redisCacheService;
    private final String CACHE_KEY = "product_related_cache_key";

    public ProductRelatedProcess (RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (redisCacheService.checkExistsKey(CACHE_KEY)) {
                    //b1: lay trong queue
                    Customer customer = (Customer)redisCacheService.rPop(CACHE_KEY);
                    //b2 Lay ra danh sach id lien quan toi product : cung category
                    List<Integer> relatedProducts = Arrays.asList(1,3,4,5);
                    //b3: luu thong tin len redis cache
                    redisCacheService.setValue("related_product_"+customer.getId(), relatedProducts);

                } else {
                    Thread.sleep(100);
                }
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
