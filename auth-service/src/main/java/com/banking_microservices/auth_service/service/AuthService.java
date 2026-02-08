package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.LoginRequestDto;
import com.banking_microservices.auth_service.dto.TokenResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AuthService {

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    // Todo: Bunlar KeyCloack'da Client olusturdugumuzda otomatik olarak eklenilen Token olusturma ve Logout Endpointleri. Bunlar Kubernetten de gonderilebilir. Ama otomatik olusturuldugu icin gerek kalmiyor.
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "/realms/{realm}/protocol/openid-connect/logout";



    public TokenResponseDto postKeycloack(Map< String, String > map){ // key de string value de string
        return RestClient.create(keycloakUrl).post()
                .uri(TOKEN_URI, realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) // keycloack sadece bunu kabul ediyor
                .body(toMultiValueMap(map))
                .retrieve()
                .body(TokenResponseDto.class);// Body i token response olarak aliyor
    }


    // KeyCloack MultiValueMap Object almasi lazim. sadece onu kabul ediyor. o yuzden aldigimiz Map'ı MultiValueMap'e cevırıyoruz
    private MultiValueMap<String, String> toMultiValueMap(Map<String, String> map) {
        var multiValueMap = new LinkedMultiValueMap<String, String>();
        map.forEach(multiValueMap::add); // bitene kadar ekliyor
        return multiValueMap;
    }


}
