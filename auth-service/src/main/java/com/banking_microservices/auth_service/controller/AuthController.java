package com.banking_microservices.auth_service.controller;

import com.banking_microservices.auth_service.dto.*;
import com.banking_microservices.auth_service.service.AuthService;
import com.banking_microservices.auth_service.service.KeycloackUserService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth-service/v1/auth")
public class AuthController {


    private final KeycloackUserService keycloackUserService;
    private final AuthService authService;

    public AuthController(KeycloackUserService keycloackUserService, AuthService authService) {
        this.keycloackUserService = keycloackUserService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginEndpoint(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(keycloackUserService.login(loginRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefleshTokenRequestDto> refreshTokenEndpoint(
            @Valid @RequestBody RefleshTokenRequestDto request) {
        return ResponseEntity.ok(keycloackUserService.refleshTokenWithRefleshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutEndpoint(@Valid @RequestBody LogOutRequestDto logoutRequest) {
        keycloackUserService.logOut(logoutRequest.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("register")
    public ResponseEntity<?> registerEndpoint(@Valid @RequestBody RegisterDto requestdto) {
        authService.createUser(requestdto);
        return ResponseEntity.ok().build();
    }

}
