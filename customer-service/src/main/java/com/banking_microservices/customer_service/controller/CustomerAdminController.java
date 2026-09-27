package com.banking_microservices.customer_service.controller;

import com.banking_microservices.customer_service.dto.CreateCustomerRequest;
import com.banking_microservices.customer_service.dto.CustomerContactVerificationRequest;
import com.banking_microservices.customer_service.dto.CustomerKycUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerMfaUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerProfileUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.dto.CustomerRiskUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerSpecialUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerStatusUpdateRequest;
import com.banking_microservices.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer-service/v1/admin/customers")
@RequiredArgsConstructor
public class CustomerAdminController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerReadDto> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerReadDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @GetMapping("/keycloak/{keycloakId}")
    public ResponseEntity<CustomerReadDto> getByKeycloakId(@PathVariable String keycloakId) {
        return ResponseEntity.ok(customerService.getByKeycloakId(keycloakId));
    }

    @GetMapping("/email")
    public ResponseEntity<CustomerReadDto> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerService.getByEmail(email));
    }

    @GetMapping("/phone")
    public ResponseEntity<CustomerReadDto> getByPhoneNumber(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(customerService.getByPhoneNumber(phoneNumber));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerReadDto>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerService.search(keyword, limit));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomerReadDto>> listByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerService.listByStatus(status, limit));
    }

    @GetMapping("/kyc/{kycStatus}")
    public ResponseEntity<List<CustomerReadDto>> listByKycStatus(
            @PathVariable String kycStatus,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerService.listByKycStatus(kycStatus, limit));
    }

    @GetMapping("/special")
    public ResponseEntity<List<CustomerReadDto>> listSpecialCustomers(@RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerService.listSpecialCustomers(limit));
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<CustomerReadDto>> listHighRiskCustomers(
            @RequestParam(defaultValue = "70") int minRiskScore,
            @RequestParam(defaultValue = "40") int limit) {
        return ResponseEntity.ok(customerService.listHighRiskCustomers(minRiskScore, limit));
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerReadDto> updateProfile(@Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(request));
    }

    @PatchMapping("/contact-verification")
    public ResponseEntity<CustomerReadDto> updateContactVerification(@Valid @RequestBody CustomerContactVerificationRequest request) {
        return ResponseEntity.ok(customerService.updateContactVerification(request));
    }

    @PatchMapping("/status")
    public ResponseEntity<CustomerReadDto> updateStatus(@Valid @RequestBody CustomerStatusUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateStatus(request));
    }

    @PatchMapping("/kyc")
    public ResponseEntity<CustomerReadDto> updateKycStatus(@Valid @RequestBody CustomerKycUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateKycStatus(request));
    }

    @PatchMapping("/risk")
    public ResponseEntity<CustomerReadDto> updateRiskScore(@Valid @RequestBody CustomerRiskUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateRiskScore(request));
    }

    @PatchMapping("/mfa")
    public ResponseEntity<CustomerReadDto> updateMfaPreference(@Valid @RequestBody CustomerMfaUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateMfaPreference(request));
    }

    @PatchMapping("/special")
    public ResponseEntity<CustomerReadDto> updateSpecialCustomer(@Valid @RequestBody CustomerSpecialUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateSpecialCustomer(request));
    }

    @DeleteMapping
    public ResponseEntity<CustomerReadDto> softDeleteCustomer(@RequestParam String id) {
        return ResponseEntity.ok(customerService.softDeleteCustomer(id));
    }
}
