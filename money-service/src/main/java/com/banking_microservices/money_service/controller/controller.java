package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.IdDto;
import com.banking_microservices.money_service.repository.repository;
import com.banking_microservices.money_service.service.service;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class controller {

    private final Gson gson = new Gson();
    private final repository repository;
    private final service service;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Money service is healty");
    }

    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@Valid @RequestBody IdDto userId){
        String gsonLog = gson.toJson(userId);
        log.info("User Id Parametresi geldi. {}" , gsonLog);
        return ResponseEntity.ok(service.generateUser(userId.getId()));
    }

}
