package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.IdDto;
import com.banking_microservices.money_service.repository.repository;
import com.banking_microservices.money_service.service.service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class controller {


    private final repository repository;
    private final service service;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Money service is healty");
    }

    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@NotNull @RequestBody String userId){
        return ResponseEntity.ok(service.generateUser(userId));
    }

}
