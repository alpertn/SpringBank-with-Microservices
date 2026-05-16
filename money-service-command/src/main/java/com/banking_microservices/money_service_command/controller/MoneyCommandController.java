package com.banking_microservices.money_service_command.controller;

import com.banking_microservices.money_service_command.dto.*;
import com.banking_microservices.money_service_command.service.MoneyCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/money-service-command/v1/accounts")
@RequiredArgsConstructor
public class MoneyCommandController {

    // Bu controller sadece write-side endpointlerini acar.
    // Query endpointleri burada degil money-service-query tarafinda tutulur.
    private final MoneyCommandService moneyCommandService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("money-service-command is healthy");
    }

    @PostMapping
    public ResponseEntity<MoneyAccountResponseDto> createAccount(@Valid @RequestBody CreateMoneyAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moneyCommandService.createAccount(request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<MoneyAccountResponseDto> deposit(@Valid @RequestBody BalanceCommandRequest request) {
        return ResponseEntity.ok(moneyCommandService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<MoneyAccountResponseDto> withdraw(@Valid @RequestBody BalanceCommandRequest request) {
        return ResponseEntity.ok(moneyCommandService.withdraw(request));
    }

    @PostMapping("/block-money")
    public ResponseEntity<MoneyAccountResponseDto> blockMoney(@Valid @RequestBody BlockMoneyCommandRequest request) {
        return ResponseEntity.ok(moneyCommandService.blockMoney(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<MoneyAccountResponseDto> executeTransfer(@Valid @RequestBody TransferCommandRequest request) {
        return ResponseEntity.ok(moneyCommandService.executeTransfer(request));
    }
}
