package com.banking_microservices.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

@Configuration
public class GatewayConfig {

    // Ozet Olarak neden kendi get ip methodumu yazmadigim :
    // Spring'in kendi sınıfı. Görevi şu: client'tan gelen request'teki X-Forwarded-For, X-Forwarded-Host gibi header'ları okuyup getRemoteAddress() üzerine yazar. Yani sen hiç header parse etmeden direkt getRemoteAddress() çağırınca gerçek IP'yi alırsın
    @Bean
    public ForwardedHeaderTransformer forwardedHeaderTransformer() {
        return new ForwardedHeaderTransformer();
    }


}