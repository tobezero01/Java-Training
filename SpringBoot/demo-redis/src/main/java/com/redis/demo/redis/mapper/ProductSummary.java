package com.redis.demo.redis.mapper;

public record ProductSummary(
        Integer id,
        String name,
        float price,
        String mainImage
){}