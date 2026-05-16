package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.user.ChangeEmailRequestDto;
import com.banking_microservices.user_service.dto.user.ChangePasswordRequestDto;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.service.UserService;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;

@RestController
@Slf4j
@RequestMapping("/api/user-service/v1/user")
public class UserController {

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Users> getUser(@PathVariable String userId) {
        log.info("user endpoint request {}", userId);
        return ResponseEntity.ok(userService.findUserById(userId));
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Void> changePassword(@PathVariable String userId, @Valid @RequestBody ChangePasswordRequestDto requestDto) {
        userService.changePassword(userId, requestDto.getCurrentPassword(), requestDto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/change-email")
    public ResponseEntity<Void> changeEmail(@PathVariable String userId, @Valid @RequestBody ChangeEmailRequestDto requestDto) {
        userService.changeEmail(userId, requestDto.getNewEmail(), requestDto.getPassword());
        return ResponseEntity.ok().build();
    }

}
