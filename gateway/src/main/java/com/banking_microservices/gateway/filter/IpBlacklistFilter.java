package com.banking_microservices.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@RequiredArgsConstructor
@Component
@Slf4j
@Order(-100) // order anatasyonu sayinin degeri en az olan ilk calistirilir
public class IpBlacklistFilter implements GlobalFilter { //GlobalFilter implemetasyonu  istek geldigi gibi bunu calistirir

    private final ReactiveRedisTemplate<String,String> redisTemplate;

    @Bean
}
//
/// **
// * IP Blacklist Filter
// * - Redis'te "ip:blacklist" set'inde olan IP'leri engeller
// * - Localhost bypass edilir
// * - Order: -100 (en önce çalışır)
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class IpBlacklistFilter implements GlobalFilter, Ordered {
//
//    private final ReactiveRedisTemplate<String, String> redisTemplate;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        String ip = getIp(exchange);
//
//        // Localhost bypass
//        if (ip.equals("127.0.0.1") || ip.equals("::1")) {
//            return chain.filter(exchange);
//        }
//
//        // Redis kontrolü
//        return redisTemplate.opsForSet().isMember("ip:blacklist", ip)
//                .flatMap(blocked -> {
//                    if (Boolean.TRUE.equals(blocked)) {
//                        log.warn("IP engellendi: {}", ip);
//                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
//                        return exchange.getResponse().setComplete();
//                    }
//                    return chain.filter(exchange);
//                });
//    }
//
//    // IP çözümleme: X-Forwarded-For veya RemoteAddress
//    private String getIp(ServerWebExchange ex) {
//        String xff = ex.getRequest().getHeaders().getFirst("X-Forwarded-For");
//        if (xff != null && !xff.isBlank()) {
//            return xff.split(",")[0].trim();
//        }
//        var addr = ex.getRequest().getRemoteAddress();
//        if (addr != null) {
//            return addr.getAddress().getHostAddress();
//        }
//        return "unknown";
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    } // En önce çalışır
//}