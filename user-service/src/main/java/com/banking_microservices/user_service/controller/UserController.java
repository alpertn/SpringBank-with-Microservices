package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.user.ChangeEmailRequestDto;
import com.banking_microservices.user_service.dto.user.ChangePasswordRequestDto;
import com.banking_microservices.user_service.service.UserService;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.banking_microservices.user_service.dto.user.UsersDto;
import com.google.gson.Gson;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/createuser")
    public ResponseEntity<UsersDto> createUser(@Valid @RequestBody UsersDto usersDto) {
        log.info("createuser endpointine gelen request {}", gson.toJson(usersDto));
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.saveUser(usersDto));
    }

    // Şifre Değiştirme
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable String userId,
            @Valid @RequestBody ChangePasswordRequestDto requestDto) {
        log.info("Password change request for user ID: {}", userId);
        userService.changePassword(userId, requestDto.getCurrentPassword(), requestDto.getNewPassword());
        log.info("Password changed successfully for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }

    // Email Değiştirme
    @PostMapping("/{userId}/change-email")
    public ResponseEntity<?> changeEmail(@PathVariable String userId,
            @Valid @RequestBody ChangeEmailRequestDto requestDto) {
        log.info("Email change request for user ID: {}", userId);
        userService.changeEmail(userId, requestDto.getNewEmail(), requestDto.getPassword());
        log.info("Email changed successfully for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }

}
