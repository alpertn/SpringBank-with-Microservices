package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.IdDto;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.banking_microservices.money_service.service.UserMoneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class MoneyController {

    private final Gson gson = new Gson();
    private final UserMoneyRepository UserMoneyRepository;
    private final UserMoneyService UserMoneyService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UserMoney UserMoneyService is healthy");
    }

    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@Valid @RequestBody IdDto userId) {
        String gsonLog = gson.toJson(userId);
        log.info("User Id Parametresi geldi. {}", gsonLog);
        return ResponseEntity.ok(UserMoneyService.generateUser(userId.getId()));
    }

}
