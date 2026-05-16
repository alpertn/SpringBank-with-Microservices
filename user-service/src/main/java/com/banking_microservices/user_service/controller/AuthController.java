package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.auth.LoginRequestDto;
import com.banking_microservices.user_service.dto.auth.RefleshTokenRequestDto;
import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.dto.auth.TokenResponseDto;
import com.banking_microservices.user_service.service.UserAuthService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/user-service/v1/auth")
@Slf4j
public class AuthController {

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private final UserAuthService userAuthService;
    private final Supplier<String> currentTime;

    public AuthController(UserAuthService userAuthService, Supplier<String> currentTime) {
        this.userAuthService = userAuthService;
        this.currentTime = currentTime;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginEndpoint(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        log.info(" ({}) > AuthController | login -> {}", currentTime.get(), gson.toJson(loginRequestDto));
        return ResponseEntity.ok(userAuthService.login(loginRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refreshTokenEndpoint(@Valid @RequestBody RefleshTokenRequestDto dto) {
        log.info(" ({}) > AuthController | refresh -> request received", currentTime.get());
        return ResponseEntity.ok(userAuthService.refresh(dto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutEndpoint(@Valid @RequestBody RefleshTokenRequestDto dto) {
        log.info(" ({}) > AuthController | logout -> request received", currentTime.get());
        userAuthService.logout(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerEndpoint(@Valid @RequestBody RegisterDto dto) {
        log.info(" ({}) > AuthController | register -> {}", currentTime.get(), gson.toJson(dto));
        userAuthService.register(dto);
        return ResponseEntity.ok().build();
    }
}
