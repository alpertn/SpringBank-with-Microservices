package com.banking_microservices.customer_service.controller;

import com.banking_microservices.customer_service.dto.CustomerMfaUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerProfileUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.exception.CustomerAccessDeniedException;
import com.banking_microservices.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-service/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service is healthy");
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerReadDto> me(@RequestHeader("X-User-KeycloakUUID") String keycloakId) {
        return ResponseEntity.ok(customerService.getByKeycloakId(keycloakId));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<CustomerReadDto> updateMyProfile(
            @RequestHeader("X-User-KeycloakUUID") String keycloakId,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        assertCurrentCustomer(keycloakId, request.id());
        return ResponseEntity.ok(customerService.updateProfile(request));
    }

    @PatchMapping("/me/mfa")
    public ResponseEntity<CustomerReadDto> updateMyMfa(
            @RequestHeader("X-User-KeycloakUUID") String keycloakId,
            @Valid @RequestBody CustomerMfaUpdateRequest request) {
        assertCurrentCustomer(keycloakId, request.id());
        return ResponseEntity.ok(customerService.updateMfaPreference(request));
    }

    private void assertCurrentCustomer(String keycloakId, String customerId) {
        CustomerReadDto customer = customerService.getByKeycloakId(keycloakId);
        if (!customer.id().equals(customerId)) {
            throw new CustomerAccessDeniedException("Customer profile can only be changed by its owner");
        }
    }
}
