package com.banking_microservices.customer_service_command.controller;

import com.banking_microservices.customer_service_command.dto.CreateCustomerCommandRequest;
import com.banking_microservices.customer_service_command.dto.CustomerResponseDto;
import com.banking_microservices.customer_service_command.dto.UpdateContactVerificationRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerProfileRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateKycStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateMfaPreferenceRequest;
import com.banking_microservices.customer_service_command.dto.UpdateRiskScoreRequest;
import com.banking_microservices.customer_service_command.dto.UpdateSpecialCustomerRequest;
import com.banking_microservices.customer_service_command.service.CustomerCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer-service-command/v1/customers")
@RequiredArgsConstructor
public class CustomerCommandController {

    private final CustomerCommandService customerCommandService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service-command is healthy");
    }

    @PostMapping
    public ResponseEntity<CustomerResponseDto> create(@Valid @RequestBody CreateCustomerCommandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerCommandService.createCustomer(request));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> getById(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerCommandService.getById(customerId));
    }

    @GetMapping("/keycloak/{keycloakId}")
    public ResponseEntity<CustomerResponseDto> getByKeycloakId(@PathVariable UUID keycloakId) {
        return ResponseEntity.ok(customerCommandService.getByKeycloakId(keycloakId));
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerResponseDto> updateProfile(@Valid @RequestBody UpdateCustomerProfileRequest request) {
        return ResponseEntity.ok(customerCommandService.updateProfile(request));
    }

    @PutMapping("/contact-verification")
    public ResponseEntity<CustomerResponseDto> updateContactVerification(@Valid @RequestBody UpdateContactVerificationRequest request) {
        return ResponseEntity.ok(customerCommandService.updateContactVerification(request));
    }

    @PutMapping("/status")
    public ResponseEntity<CustomerResponseDto> updateStatus(@Valid @RequestBody UpdateCustomerStatusRequest request) {
        return ResponseEntity.ok(customerCommandService.updateStatus(request));
    }

    @PutMapping("/kyc")
    public ResponseEntity<CustomerResponseDto> updateKycStatus(@Valid @RequestBody UpdateKycStatusRequest request) {
        return ResponseEntity.ok(customerCommandService.updateKycStatus(request));
    }

    @PutMapping("/risk")
    public ResponseEntity<CustomerResponseDto> updateRiskScore(@Valid @RequestBody UpdateRiskScoreRequest request) {
        return ResponseEntity.ok(customerCommandService.updateRiskScore(request));
    }

    @PutMapping("/mfa")
    public ResponseEntity<CustomerResponseDto> updateMfa(@Valid @RequestBody UpdateMfaPreferenceRequest request) {
        return ResponseEntity.ok(customerCommandService.updateMfa(request));
    }

    @PutMapping("/special")
    public ResponseEntity<CustomerResponseDto> updateSpecialCustomer(@Valid @RequestBody UpdateSpecialCustomerRequest request) {
        return ResponseEntity.ok(customerCommandService.updateSpecialCustomer(request));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> softDelete(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerCommandService.softDelete(customerId));
    }
}
