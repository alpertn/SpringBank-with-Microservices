package com.banking_microservices.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
public class IpBlacklistFilter implements GlobalFilter { // GlobalFilter implemetasyonu istek geldigi gibi bunu  calistirir

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // Mono kullanmamin nedeni Webfluxda veri gelince isle beklemeden devam et methodunu kullanir reaktif programlama denir buna. oyuzden Mono<Void> kullandm
    @Override // Override ust siniftaki method veya interfacenin configini yazmak gibidir. GlobalFilter in configi yani.
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { // Chain devam etmek onay vermek icin.ServerWebExchange de Tum istekler oluyor. gelen tum istek bu. ip header vesaire.

        String ip = getIpWithGatewayConfig(exchange);

        if (isLocalhost(ip)) {
            return chain.filter(exchange); // kabul ediyoruz istegi ve istegi gonderiyoruz chaine.

        }
        // ip:blacklist setinde bu ip var mi diye sorguluyor
        return redisTemplate.opsForSet().isMember("ip:blacklist", ip)
                .flatMap(trueOrElseRedisResponse -> {
                    if (trueOrElseRedisResponse) {
                        return sendBlacklistedIpResponseAndCompleteRequest(exchange, ip); // chaini baslatmadan requesti kesmek icin bunu calistiriyor responseyi de yaziyor
                    } else {
                        return chain.filter(exchange); // eger blacklist degilse chaini baslatiyor.
                    }
                });

    }

    // ex.getRequest yaparken aslinda GatewayConfigde @Bean olrak tanilmadigimiz icin kolayca constructor injection yapmadan kullaniyoruz. classi bile olusturmuyoruz. Spring sadece containere bakiyor varmi yokmu varsa otomatik algiliyor.

    private String getIpWithGatewayConfig(ServerWebExchange ex) {
        var addr = ex.getRequest().getRemoteAddress();
        if (addr != null) {
            return addr.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private boolean isLocalhost(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return true;
        } else {
            return false;
        }
    }

    private Mono<Void> sendBlacklistedIpResponseAndCompleteRequest(ServerWebExchange exchange, String ip) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
