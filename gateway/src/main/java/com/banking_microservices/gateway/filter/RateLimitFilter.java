package com.banking_microservices.gateway.filter;

import reactor.core.publisher.Mono;

public class RateLimitFilter {
    private Mono<Long> setTTLDurationCount(String rediskey)
}
//    private Mono<Long> setTtlIfFirstRequest(String redisKey, Long count) {
//        if (count == 1) {
//            return redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS))
//                    .thenReturn(count); // TTL koyduktan sonra count'u döndür
//        }
//        return Mono.just(count); // İlk istek değilse direkt count döndür
//    }