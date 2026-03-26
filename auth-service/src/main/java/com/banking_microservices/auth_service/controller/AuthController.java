package com.banking_microservices.auth_service.controller;

import com.banking_microservices.auth_service.dto.*;
import com.banking_microservices.auth_service.service.AuthService;
import com.banking_microservices.auth_service.service.KeycloackUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/auth-service/v1/auth")
@Slf4j
public class AuthController {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    private final Supplier<String> currentTime;
    private final KeycloackUserService keycloackUserService;
    private final AuthService authService;

    public AuthController(KeycloackUserService keycloackUserService, AuthService authService, Supplier<String> currentTime) {
        this.keycloackUserService = keycloackUserService;
        this.authService = authService;
        this.currentTime = currentTime;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginEndpoint(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        log.info(" ({}) > AuthController | loginEndpoint -> Login istegi alindi. Dto:\n{}", currentTime.get(), gson.toJson(loginRequestDto));
        return ResponseEntity.ok(keycloackUserService.login(loginRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefleshTokenRequestDto> refreshTokenEndpoint(@Valid @RequestBody RefleshTokenRequestDto refleshTokenRequestDto) {
        log.info(" ({}) > AuthController | refreshTokenEndpoint -> Refresh token istegi alindi. Dto:\n{}", currentTime.get(), gson.toJson(refleshTokenRequestDto));
        return ResponseEntity.ok(keycloackUserService.refleshTokenWithRefleshToken(refleshTokenRequestDto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutEndpoint(@Valid @RequestBody LogOutRequestDto logoutRequest) {
        log.info(" ({}) > AuthController | logoutEndpoint -> Logout istegi alindi. Dto:\n{}", currentTime.get(), gson.toJson(logoutRequest));
        keycloackUserService.logOut(logoutRequest.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerEndpoint(@Valid @RequestBody RegisterDto requestdto) {
        log.info(" ({}) > AuthController | registerEndpoint -> Register istegi alindi. Dto:\n{}", currentTime.get(), gson.toJson(requestdto));
        authService.createUser(requestdto);
        return ResponseEntity.ok().build();
    }
}
