package com.banking_microservices.money_service_query.controller;

import com.banking_microservices.money_service_query.dto.MoneyAccountReadDto;
import com.banking_microservices.money_service_query.service.MoneyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/money-service-query/v1/accounts")
@RequiredArgsConstructor
public class MoneyQueryController {

    // Bu controller sadece read-side endpointleri sunar.
    // Command operasyonlari burada bilincli olarak yer almaz.
    private final MoneyQueryService moneyQueryService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("money-service-query is healthy");
    }

    @GetMapping("/{id}")
    public ResponseEntity<MoneyAccountReadDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(moneyQueryService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<MoneyAccountReadDto> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(moneyQueryService.getByUserId(userId));
    }

    @GetMapping("/iban/{iban}")
    public ResponseEntity<MoneyAccountReadDto> getByIban(@PathVariable String iban) {
        return ResponseEntity.ok(moneyQueryService.getByIban(iban));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MoneyAccountReadDto>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(moneyQueryService.search(keyword));
    }
}
