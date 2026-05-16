package com.banking_microservices.user_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ProcessedEventStore {

    private static final Duration EVENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public ProcessedEventStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean markIfFirst(String eventType, String eventId) {
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(buildKey(eventType, eventId), "1", EVENT_TTL);
        return Boolean.TRUE.equals(created);
    }

    private String buildKey(String eventType, String eventId) {
        return "user-service:event:" + eventType + ":" + eventId;
    }
}
