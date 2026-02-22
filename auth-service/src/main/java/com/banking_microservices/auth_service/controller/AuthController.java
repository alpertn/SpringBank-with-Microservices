package com.banking_microservices.auth_service.controller;

import com.banking_microservices.auth_service.dto.*;
import com.banking_microservices.auth_service.service.AuthService;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginEndpoint(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefleshTokenRequestDto> refreshTokenEndpoint(
            @Valid @RequestBody RefleshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refleshTokenWithRefleshToken(request.getRefreshToken()));
    }

    // @PostMapping("/logout")
    // public ResponseEntity<Void> logoutEndpoint(@Valid @RequestBody
    // LogOutRequestDto logoutRequest) {
    // authService.logOut(logoutRequest.getRefreshToken());
    // return ResponseEntity.ok().build();
    // }

    @PostMapping("register")
    public ResponseEntity<?> registerEndpoint(@Valid @RequestBody RegisterDto requestdto) {
        authService.createUser(requestdto);
        return ResponseEntity.ok().build();
    }

}
