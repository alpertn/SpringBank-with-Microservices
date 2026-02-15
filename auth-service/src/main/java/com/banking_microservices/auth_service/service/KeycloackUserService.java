package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.LoginRequestDto;
import com.banking_microservices.auth_service.dto.RefleshTokenRequestDto;
import com.banking_microservices.auth_service.dto.TokenResponseDto;
import com.banking_microservices.auth_service.exception.InvalidTokenException;
import com.banking_microservices.auth_service.exception.KeycloakConnectionException;
import com.banking_microservices.auth_service.exception.LoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class KeycloackUserService {

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;


    // Todo: Bunlar KeyCloack'da Client olusturdugumuzda otomatik olarak eklenilen
    // Token olusturma ve Logout Endpointleri. Bunlar Kubernetten de gonderilebilir.
    // Ama otomatik olusturuldugu icin gerek kalmiyor.
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "/realms/{realm}/protocol/openid-connect/logout";

    // keycloackdan aldigi tokeni dondurur.
    public TokenResponseDto login(LoginRequestDto loginRequest) {

        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "password");
        params.put("username", loginRequest.getEmail());
        params.put("password", loginRequest.getPassword());

        try {
            return RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm) // uri
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED) // request media type
                    .body(toMultiValueMap(params)) // keycloack icin uyumlu hale getir
                    .retrieve() // response
                    .body(TokenResponseDto.class); // body'ı tokenresponsedto ya donustur
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new LoginException("Invalid mail or password.");
        } catch (ResourceAccessException e) {
            throw new KeycloakConnectionException("Keycloak serverine erisim saglanamadi");
        }
    }

    // KeyCloack MultiValueMap Object almasi lazim. sadece onu kabul ediyor. o
    // yuzden aldigimiz Map'ı MultiValueMap'e cevırıyoruz
    private MultiValueMap<String, String> toMultiValueMap(Map<String, String> map) {
        var multiValueMap = new LinkedMultiValueMap<String, String>();
        map.forEach(multiValueMap::add); // bitene kadar ekliyor
        return multiValueMap;
    }

    // eski tokeni gonderip yenisini aliyor.
    public RefleshTokenRequestDto refleshTokenWithRefleshToken(String refleshToken) {

        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refleshToken);

        try {
            return RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm) // uri
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .body(RefleshTokenRequestDto.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidTokenException("Refresh tokenin suresi bitti. yeniden token al.");

        } catch (ResourceAccessException e) {
            throw new KeycloakConnectionException("Keycloack serverine ulasilamiyor.");
        }
    }

    // Logout URI Istek atiyor.
    public void logOut(String refleshToken) { // Token silme
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refleshToken);

        try {
            RestClient.create(keycloakUrl).post()
                    .uri(LOGOUT_URI, realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidTokenException("Token gecersiz.");
        } catch (ResourceAccessException e) {
            throw new KeycloakConnectionException("Keycloack serverine baglanilamadi.");
        }
    }




}