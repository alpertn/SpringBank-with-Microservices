package com.banking_microservices.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * SSE Endpoint Token Web Filter
 *
 * Browser'in EventSource API'si custom HTTP header gonderemiyor.
 * Bu yuzden SSE log endpoint'leri icin token query parameter olarak gonderilir:
 *   /api/gateway/admin/logs/{service}?token=eyJhb...
 *
 * Bu filter, SSE isteklerinde "token" query param'ini alip
 * "Authorization: Bearer {token}" header'i olarak inject eder.
 * Boylece Spring Security JWT dogrulamasi normal calisir.
 *
 * Guvenlik: sadece /api/gateway/admin/logs/ path'ine uygulaniyor.
 *
 * @Order(Ordered.HIGHEST_PRECEDENCE) Spring Security'den once calismasini saglar.
 * Aksi halde token inject edilmeden JWT kontrolu yapilir ve 401 donulur.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class SseTokenWebFilter implements WebFilter {

    private static final String TOKEN_PARAM = "token";
    private static final String SSE_LOG_PATH = "/api/gateway/admin/logs/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Sadece SSE log endpoint'lerine uygula
        if (!path.startsWith(SSE_LOG_PATH)) {
            return chain.filter(exchange);
        }

        // Authorization header zaten varsa dokununma
        if (request.getHeaders().containsKey("Authorization")) {
            return chain.filter(exchange);
        }

        // Query param'dan token'i al
        String token = request.getQueryParams().getFirst(TOKEN_PARAM);
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        // Token'i Authorization header'a inject et
        ServerHttpRequest mutated = request.mutate()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
