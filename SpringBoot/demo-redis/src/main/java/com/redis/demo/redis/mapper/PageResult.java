package com.redis.demo.redis.mapper;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        boolean hasNext
) {}
