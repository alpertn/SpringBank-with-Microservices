package com.banking_microservices.gateway.filter;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * SSE (Server-Sent Events) baglantilari icin query parameter'dan JWT token okur.
 *
 * Sorun: Tarayicilarin EventSource API'si Authorization header gondermez.
 * Cozum: Frontend token'i ?token=... seklinde URL'ye ekler, bu filtre onu
 * Authorization header'ina tasir. Boylece Spring Security normal JWT dogrulamasi yapar.
 *
 * Sadece /api/gateway/admin/logs/ yollarinda aktiftir — diger endpointler etkilenmez.
 * 
 * Oncelik: -100 (SecurityWebFilterChain'den ONCE calisir)
 */
@Order(-100)
@Component
public class SseTokenQueryParamFilter implements WebFilter {

    private static final String LOG_PATH_PREFIX = "/api/gateway/admin/logs/";
    private static final String TOKEN_PARAM = "token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Sadece log stream endpoint'leri icin calis
        if (!path.startsWith(LOG_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        // Authorization header zaten varsa dokunma
        if (request.getHeaders().containsKey("Authorization")) {
            return chain.filter(exchange);
        }

        // Query parameter'dan token'i al
        String token = request.getQueryParams().getFirst(TOKEN_PARAM);
        if (token != null && !token.isBlank()) {
            // Token'i Authorization header olarak ekle
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }
}
