package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.LoginRequestDto;
import com.banking_microservices.auth_service.dto.RefleshTokenRequestDto;
import com.banking_microservices.auth_service.dto.TokenResponseDto;
import com.banking_microservices.auth_service.exception.InvalidTokenException;
import com.banking_microservices.auth_service.exception.KeycloakConnectionException;
import com.banking_microservices.auth_service.exception.LoginException;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Login Register gibi Keycloak islemlerinin yapildigi
 * Admin apisine gefrek kalmadan yapilabilen
 * projeye eklenmis tum islemlerin methodlari buradadir.
 *
 * Login
 * refleshTokenWithRefleshToken
 * logout
 * gibi methodlar vardir.
 *
 */

@Service
@Slf4j
public class KeycloackUserService {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private final Supplier<String> currentTime;

    public KeycloackUserService(Supplier<String> currentTime) {
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

    // Default Keycloak URI
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "/realms/{realm}/protocol/openid-connect/logout";


    /**
     * Called By {@link com.banking_microservices.auth_service.controller.AuthController}
     *
     * keycloakin Login apilerine istek gonderir.
     *
     * @param loginRequest {@link com.banking_microservices.auth_service.controller.AuthController} den aldigi veri
     * @return {@link TokenResponseDto} token dondurur.
     * @throws InvalidTokenException HttpClientErrorException.Unauthorized Expired veya invalid token.
     * @throws KeycloakConnectionException ResourceAccessException aciga ciktiginda keycloackConnectionda hata vardir oyuzden daha anlasilir bir sekilde exception firlattim.
     */

    public TokenResponseDto login(LoginRequestDto loginRequest) {
        log.info(" ({}) > KeycloackUserService | login -> Login islemi basladi. Request: {}", currentTime.get(), gson.toJson(loginRequest));
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "password");
        params.put("username", loginRequest.getEmail());
        params.put("password", loginRequest.getPassword());

        try {
            log.info(" ({}) > KeycloackUserService | login -> Keycloak'a token istegi atiliyor.", currentTime.get());
            TokenResponseDto response = RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm) // uri
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED) // request media type
                    .body(toMultiValueMap(params)) // keycloack icin uyumlu hale getir
                    .retrieve() // response
                    .body(TokenResponseDto.class); // body'ı tokenresponsedto ya donustur
            log.info(" ({}) > KeycloackUserService | login -> Login basarili. Token alindi. {}", currentTime.get(), gson.toJson(response));
            return response;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn(" ({}) > KeycloackUserService | login -> Login basarisiz! Gecersiz mail veya sifre. Email: {}", currentTime.get(), loginRequest.getEmail());
            throw new LoginException("Invalid mail or password.");
        } catch (ResourceAccessException e) {
            log.warn(" ({}) > KeycloackUserService | login -> Keycloak serverine erisim saglanamadi! Hata: {}", currentTime.get(), e);
            throw new KeycloakConnectionException("Keycloak serverine erisim saglanamadi");
        }
    }


    /**
     * Formatter.
     * Aldıgımız Map verıyı multıvalueMap'e cevırıyoruz.
     * KeyCloack MultiValueMap Object almasi lazim. sadece onu kabul ediyor. o
     * yuzden aldigimiz Map'ı MultiValueMap'e cevırıyoruz
      * @param map
     * @return {@link MultiValueMap<String, String>}
     */

    private MultiValueMap<String, String> toMultiValueMap(Map<String, String> map) {
        var multiValueMap = new LinkedMultiValueMap<String, String>();
        map.forEach(multiValueMap::add); // bitene kadar ekliyor
        return multiValueMap;
    }


    /**
     *  refresh token alıyor ve access token olusturup donduruyor.
     *
     * @param refleshToken
     * @return
     * @throws InvalidTokenException HttpClientErrorException.Unauthorized Expired veya invalid token.
     * @throws KeycloakConnectionException ResourceAccessException aciga ciktiginda keycloackConnectionda hata vardir oyuzden daha anlasilir bir sekilde exception firlattim.
     */

    public RefleshTokenRequestDto refleshTokenWithRefleshToken(String refleshToken) {
        log.info(" ({}) > KeycloackUserService | refleshTokenWithRefleshToken -> Refresh token islemi basladi. Token: {}", currentTime.get(), refleshToken);
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refleshToken);

        try {
            log.info(" ({}) > KeycloackUserService | refleshTokenWithRefleshToken -> Keycloak'a refresh token istegi atiliyor.", currentTime.get());
            RefleshTokenRequestDto response = RestClient.create(keycloakUrl).post()
                    .uri(TOKEN_URI, realm) // uri
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .body(RefleshTokenRequestDto.class);
            log.info(" ({}) > KeycloackUserService | refleshTokenWithRefleshToken -> Refresh token basarili. {}", currentTime.get(), gson.toJson(response));
            return response;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn(" ({}) > KeycloackUserService | refleshTokenWithRefleshToken -> Refresh token suresi bitti.", currentTime.get());
            throw new InvalidTokenException("Refresh tokenin suresi bitti. yeniden token al.");

        } catch (ResourceAccessException e) {
            log.warn(" ({}) > KeycloackUserService | refleshTokenWithRefleshToken -> Keycloack serverine ulasilamiyor! Hata: {}", currentTime.get(), e);
            throw new KeycloakConnectionException("Keycloack serverine ulasilamiyor.");
        }
    }


    /**
     * Keycloak'ın Logout URI Istek atiyor. logOut islemi ile Keycloak tokeni siliyor.
     * @param refleshToken
     * @throws KeycloakConnectionException ResourceAccessException Duz keycloackConnection Exceptiondur.
     * @throws InvalidTokenException HttpClientErrorException.Unauthorized Expired veya invalid token.
     */

    public void logOut(String refleshToken) { // Token silme
        log.info(" ({}) > KeycloackUserService | logOut -> Logout islemi basladi. Token: {}", currentTime.get(), refleshToken);
        var params = new HashMap<String, String>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refleshToken);

        try {
            log.info(" ({}) > KeycloackUserService | logOut -> Keycloak'a logout istegi atiliyor.", currentTime.get());
            RestClient.create(keycloakUrl).post()
                    .uri(LOGOUT_URI, realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(toMultiValueMap(params))
                    .retrieve()
                    .toBodilessEntity();
            log.info(" ({}) > KeycloackUserService | logOut -> Logout basarili.", currentTime.get());
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn(" ({}) > KeycloackUserService | logOut -> Logout basarisiz! Token gecersiz.", currentTime.get());
            throw new InvalidTokenException("Token gecersiz.");
        } catch (ResourceAccessException e) {
            log.warn(" ({}) > KeycloackUserService | logOut -> Keycloack serverine baglanilamadi! Hata: {}", currentTime.get(), e);
            throw new KeycloakConnectionException("Keycloack serverine baglanilamadi.");
        }
    }

}