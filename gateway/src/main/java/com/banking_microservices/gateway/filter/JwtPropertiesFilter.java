package com.banking_microservices.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Order(-60)
@Component
public class JwtPropertiesFilter {
    // icinden ip de cekilip eklenilecek.
    // hepsi bir liste halinde de dondurulebilir.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        return ReactiveSecurityContextHolder.getContext() // ilk keycloacka istek gonderdigimizde ve aldigimizda bunlar spring securityde ddepolaniyor.bu bilgileri ondan aliyor.
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .ofType(JwtAuthenticationToken.class)// eger jwt ise al yoksa hata
                .map(JwtAuthenticationToken::getToken) // jwt tokendeki tokeni al. asil olan jwtnin icindeki bilgiler.
                .flatMap(jwt -> {

                    String id = jwt.getClaimAsString("sub"); // keycloackdaki id bu. jwtden cekiyo degistirilebilir
                    String keycloakUsername = jwt.getClaimAsString("preferred_username"); // degistirilecek
                    String keycloakEmail = jwt.getClaimAsString("email'"); // degistirilecek
                    String keycloakName = jwt.getClaimAsString("name'"); // degistirilecek
                    String keycloakSurname = jwt.getClaimAsString("surname"); // degistirilecek

                    String rolesStr = extractRolesAsStringFormatter(jwt);


                    // requesti degistiriyoruz ve kendi eski requestten aldigimiz veirleri koyuyoruz
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", id != null ? id : "") // buna id yoksa direkt hata verme eklenilebilir
                            .header("X-User-Email", keycloakEmail != null ? keycloakEmail : "") // != olmasina ragmen neden calisiyor arastirilacak.
                            .header("X-User-Username", keycloakUsername != null ? keycloakUsername : "")
                            .header("X-User-Name", keycloakName != null ? keycloakName : "")
                            .header("X-User-Surname", keycloakSurname != null ? keycloakSurname : "")
                            .header("X-User-Roles", rolesStr)
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate() // exchangeyi yeni requeste bagla.1
                            .request(mutatedRequest)
                            .build();
                    return chain.filter(mutatedExchange);

                })
                .switchIfEmpty(chain.filter(exchange)); // bunun amaci da usttekiler bossa devam et login register icin falan veya index.html icin bu gerekli szaen auth kontrolu keycloakda yapiliyor.1

    }


    // yapay zekadan alinti formatter.
    @SuppressWarnings("unchecked")
    private String extractRolesAsStringFormatter(Jwt jwt) {

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        // JWT'deki "realm_access" claim'ini çek
        // Keycloak bu claim'e şöyle bir şey koyar:
        // {
        //   "realm_access": {
        //     "roles": ["ROLE_USER", "offline_access", "default-roles-banking"]
        //   }
        // }

        if (realmAccess == null) {
            return ""; // realm_access claim'i yoksa boş string dön
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        // Map içindeki "roles" listesini çek
        // Cast gerekiyor çünkü Map<String, Object> tipinde tutuluyor

        if (roles == null || roles.isEmpty()) {
            return ""; // Rol yoksa boş string dön
        }

        return String.join(",", roles);
        // Listeyi virgülle birleştir
        // ["ROLE_USER", "ROLE_ADMIN"] → "ROLE_USER,ROLE_ADMIN"
        // Downstream servisler bu string'i split(",") ile tekrar listeye çevirebilir
    }
}
