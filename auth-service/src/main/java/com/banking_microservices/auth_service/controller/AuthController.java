package com.banking_microservices.auth_service.controller;

import com.banking_microservices.auth_service.dto.*;
import com.banking_microservices.auth_service.service.AuthService;
import com.banking_microservices.auth_service.service.KeycloackUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auth Service icin Controller Class
 * <p>Register login refresh logout endpointleri var.
 * Tokeni Keycloakdan alir.</p>
 *
 */

@RestController
@RequestMapping("/api/auth-service/v1/auth")
public class AuthController {

    // Gson SerializeNulls ile null olan degerleri de gosterir.
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final KeycloackUserService keycloackUserService;
    private final AuthService authService;

    // constructor
    public AuthController(KeycloackUserService keycloackUserService, AuthService authService) {
        this.keycloackUserService = keycloackUserService;
        this.authService = authService;
    }

    /**
     * Kullanici Login bilgisini alir keycloaka gonderir ve token dondurur.
     *
     * <p> GlobalExceptionHandler ile internel server error yerine exception firlatir burdaki tum endpointler. </p>
     * @param loginRequestDto gelen istek parametresi
     * @return TokenResponseDto Keycloakdan aldigi tokeni dondurur.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginEndpoint(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(keycloackUserService.login(loginRequestDto));
    }

    /**
     * Token Refresh
     *
     * @param refleshTokenRequestDto istegi alir
     * @return RefleshTokenRequestDto yeni tokeni dondurur.
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefleshTokenRequestDto> refreshTokenEndpoint(@Valid @RequestBody RefleshTokenRequestDto refleshTokenRequestDto) {
        return ResponseEntity.ok(keycloackUserService.refleshTokenWithRefleshToken(refleshTokenRequestDto.getRefreshToken()));
    }


    /**
     * Keycloak a logout urisine istek atar ve keycloak tokenini siler.
     *
     * @param logoutRequest istegi alir
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutEndpoint(@Valid @RequestBody LogOutRequestDto logoutRequest) {
        keycloackUserService.logOut(logoutRequest.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * Keycloak a register istegi gonderir. Bundan sonra KafkaTopic ine User Serviceye ulasacak sekilde istek gonderir ve iki mikroservisde de kayit olusturur.
     * @param requestdto
     * @return 200 status code
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerEndpoint(@Valid @RequestBody RegisterDto requestdto) {
        authService.createUser(requestdto);
        return ResponseEntity.ok().build();
    }

}
