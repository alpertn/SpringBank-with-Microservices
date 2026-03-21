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

/**
 * Gatewaya gelen istekler ilk buraya gelir. Gateway Buraya gelen Ipleri
 * Redis uzerindeki "ip:blacklist" setinde var mi diye kontrol eder.
 * Eger varsa istegi engeller. Yoksa chaini devam ettirir.
 * Localhostu muaf tuttum test ederken sorun cıkarmasın dıye.
 */
@RequiredArgsConstructor
@Component
@Slf4j
@Order(-100) // order anatasyonu sayinin degeri en az olan ilk calistirilir
public class IpBlacklistFilter implements GlobalFilter { // GlobalFilter implemetasyonu istek geldigi gibi bunu
                                                         // calistirir

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Bu method tum classı yoneten methoddur.
     * Gelen isteğin icindeki ip adresini alir ve redis uzerindeki ip:blacklist
     * setine sorgu yapar.
     * eğer sette ip varsa isteği engeller. Yoksa chaini devam ettirir.
     *
     * @param exchange tum web ısteğı headerle beraber geldıgı yer
     * @param chain    Onay verip diger filtrelere gondermek icin kullanilir. Chain.
     * @return {@link Mono<Void>} islem bittiginde veya engellendiginde reaktif
     *         olarak tamamlanir. reaktif ne demektir ?
     *         reaktif programlama veri gelince isle beklemeden devam et fonksyonunu
     *         kullanir ve buna da reaktif programlama denir buna.
     */
    // Mono kullanmamin nedeni Webfluxda veri gelince isle beklemeden devam et
    // methodunu kullanir reaktif programlama denir buna. oyuzden Mono<Void>
    // kullandm
    @Override // Override ust siniftaki method veya interfacenin configini yazmak gibidir.
              // GlobalFilter in configi yani.
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { // Chain devam etmek onay vermek
                                                                                     // icin.ServerWebExchange de Tum
                                                                                     // istekler oluyor. gelen tum istek
                                                                                     // bu. ip header vesaire.

        String ip = getIpWithGatewayConfig(exchange);

        if (isLocalhost(ip)) {
            return chain.filter(exchange); // kabul ediyoruz istegi ve istegi gonderiyoruz chaine.

        }
        // ip:blacklist setinde bu ip var mi diye sorguluyor
        return redisTemplate.opsForSet().isMember("ip:blacklist", ip)
                .flatMap(trueOrElseRedisResponse -> {
                    if (trueOrElseRedisResponse) {
                        return sendBlacklistedIpResponseAndCompleteRequest(exchange, ip); // chaini baslatmadan requesti
                                                                                          // kesmek icin bunu
                                                                                          // calistiriyor responseyi de
                                                                                          // yaziyor
                    } else {
                        return chain.filter(exchange); // eger blacklist degilse chaini baslatiyor.
                    }
                });

    }

    /**
     * Formatter. ServerWebExchange bilgisi alır ve ip adresini dondurur.
     * 
     * 
     * @param ex Gelen istek
     * @return Clientin ip adresi string dondurur.
     */
    // ex.getRequest yaparken aslinda GatewayConfigde @Bean olrak tanilmadigimiz
    // icin kolayca constructor injection yapmadan kullaniyoruz. classi bile
    // olusturmuyoruz. Spring sadece containere bakiyor varmi yokmu varsa otomatik
    // algiliyor.

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
