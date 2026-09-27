package com.banking_microservices.customer_service_query.controller;

import com.banking_microservices.customer_service_query.dto.CustomerReadDto;
import com.banking_microservices.customer_service_query.service.CustomerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer-service-query/v1/customers")
@RequiredArgsConstructor
public class CustomerQueryController {

    private final CustomerQueryService customerQueryService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service-query is healthy");
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerReadDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerQueryService.getById(id));
    }

    @GetMapping("/keycloak/{keycloakId}")
    public ResponseEntity<CustomerReadDto> getByKeycloakId(@PathVariable String keycloakId) {
        return ResponseEntity.ok(customerQueryService.getByKeycloakId(keycloakId));
    }

    @GetMapping("/email")
    public ResponseEntity<CustomerReadDto> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerQueryService.getByEmail(email));
    }

    @GetMapping("/phone")
    public ResponseEntity<CustomerReadDto> getByPhoneNumber(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(customerQueryService.getByPhoneNumber(phoneNumber));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerReadDto>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerQueryService.search(keyword, limit));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomerReadDto>> listByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerQueryService.listByStatus(status, limit));
    }

    @GetMapping("/kyc/{kycStatus}")
    public ResponseEntity<List<CustomerReadDto>> listByKycStatus(
            @PathVariable String kycStatus,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerQueryService.listByKycStatus(kycStatus, limit));
    }

    @GetMapping("/special")
    public ResponseEntity<List<CustomerReadDto>> listSpecialCustomers(@RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerQueryService.listSpecialCustomers(limit));
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<CustomerReadDto>> listHighRiskCustomers(
            @RequestParam(defaultValue = "70") int minRiskScore,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerQueryService.listHighRiskCustomers(minRiskScore, limit));
    }
}
