package com.algotrader.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;

@Component
public class LastTrendCache {

    @Value("${cache.comfy.level}")
    private long comfyLevel;

    private final Cache<LocalDateTime, Double> cache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public void save(Double v) {

        cache.put(LocalDateTime.now(), v);
    }

    public OptionalDouble getAverage() {

        return cache.asMap()
                .values()
                .stream()
                .mapToDouble(a -> a)
                .average();
    }

    public long getCount() {

        return cache.asMap()
                .values()
                .stream()
                .mapToDouble(a -> a)
                .count();
    }

    public boolean isComfy() {
        return cache.size() > comfyLevel;
    }

    public long getSize() {
        return cache.size();
    }
}
