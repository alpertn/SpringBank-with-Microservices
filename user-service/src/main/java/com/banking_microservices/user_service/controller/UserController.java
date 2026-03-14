package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.user.ChangeEmailRequestDto;
import com.banking_microservices.user_service.dto.user.ChangePasswordRequestDto;
import com.banking_microservices.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.banking_microservices.user_service.dto.user.UsersDto;


@RestController
@Slf4j
@RequestMapping("/api/user-service/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @PostMapping("/createuser")
//    public ResponseEntity<UsersDto> createUser(@Valid @RequestBody UsersDto usersDto) {
//        log.info("createuser endpointine gelen request {}", gson.toJson(usersDto));
//        return ResponseEntity.status(HttpStatus.CREATED).body(userService.saveUser(usersDto));
//    }

//    @PostMapping("/{userId}/change-password")
//    public ResponseEntity<?> changePassword(@PathVariable String userId, @Valid @RequestBody ChangePasswordRequestDto requestDto) {
//        userService.changePassword(userId, requestDto.getCurrentPassword(), requestDto.getNewPassword());
//        return ResponseEntity.ok().build();
//    }
//
//    @PostMapping("/{userId}/change-email")
//    public ResponseEntity<?> changeEmail(@PathVariable String userId, @Valid @RequestBody ChangeEmailRequestDto requestDto) {
//        userService.changeEmail(userId, requestDto.getNewEmail(), requestDto.getPassword());
//        return ResponseEntity.ok().build();
//    }

}
