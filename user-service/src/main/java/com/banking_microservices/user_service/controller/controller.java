package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.service.service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.banking_microservices.user_service.dto.UsersDto;

@RestController
@RequestMapping("/api")
public class controller {

    private final service service;

    public controller(service service) {
        this.service = service;
    }

    @PostMapping("/createuser")
    public ResponseEntity<UsersDto> createUser(@Valid @RequestBody UsersDto usersDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveUser(usersDto)); // 201 status code donecektir
    }
}
