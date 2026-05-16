package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.auth.LoginRequestDto;
import com.banking_microservices.user_service.dto.auth.TokenResponseDto;
import com.banking_microservices.user_service.exception.InvalidTokenException;
import com.banking_microservices.user_service.exception.KeycloakConnectionException;
import com.banking_microservices.user_service.exception.LoginException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.function.Supplier;

@Service
@Slf4j
public class KeycloakUserService {

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final Supplier<String> currentTime;

    public KeycloakUserService(Supplier<String> currentTime) {
        this.currentTime = currentTime;
    }

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "/realms/{realm}/protocol/openid-connect/logout";

    public TokenResponseDto login(LoginRequestDto loginRequest) {
        log.info(" ({}) > KeycloakUserService | login -> Request:\n{}", currentTime.get(), gson.toJson(loginRequest));
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "password");
        params.put("username", loginRequest.getEmail());
        params.put("password", loginRequest.getPassword());

        try {
            return RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .body(TokenResponseDto.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new LoginException("Invalid mail or password.");
        } catch (ResourceAccessException e) {
            throw new KeycloakConnectionException("Keycloak serverine erisim saglanamadi");
        }
    }

    public TokenResponseDto refreshWithRefreshToken(String refreshToken) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        try {
            return RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .body(TokenResponseDto.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidTokenException("Refresh tokenin suresi bitti. yeniden token al.");
        } catch (ResourceAccessException e) {
            throw new KeycloakConnectionException("Keycloak serverine ulasilamiyor.");
        }
    }

    public void logout(String refreshToken) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);

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
            throw new KeycloakConnectionException("Keycloak serverine baglanilamadi.");
        }
    }

    public void verifyCredentials(String email, String password) {
        login(LoginRequestDto.builder().email(email).password(password).build());
    }

    private MultiValueMap<String, String> toMultiValueMap(Map<String, String> map) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        map.forEach(multiValueMap::add);
        return multiValueMap;
    }
}
