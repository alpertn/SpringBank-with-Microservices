package com.banking_microservices.customer_service.service;

import com.banking_microservices.customer_service.dto.CreateCustomerRequest;
import com.banking_microservices.customer_service.dto.CustomerContactVerificationRequest;
import com.banking_microservices.customer_service.dto.CustomerKycUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerMfaUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerProfileUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.dto.CustomerRiskUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerSpecialUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerStatusUpdateRequest;
import com.banking_microservices.customer_service.grpc.CustomerCommandGrpcClient;
import com.banking_microservices.customer_service.grpc.CustomerQueryGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerCommandGrpcClient commandClient;
    private final CustomerQueryGrpcClient queryClient;
    private final Supplier<String> currentTime;

    public CustomerReadDto createCustomer(CreateCustomerRequest request) {
        log.info("({}) CustomerService | createCustomer -> request received. keycloakId={}, email={}",
                currentTime.get(), request.keycloakId(), request.email());
        return commandClient.createCustomer(request);
    }

    public CustomerReadDto updateProfile(CustomerProfileUpdateRequest request) {
        return commandClient.updateProfile(request);
    }

    public CustomerReadDto updateContactVerification(CustomerContactVerificationRequest request) {
        return commandClient.updateContactVerification(request);
    }

    public CustomerReadDto updateStatus(CustomerStatusUpdateRequest request) {
        return commandClient.updateStatus(request);
    }

    public CustomerReadDto updateKycStatus(CustomerKycUpdateRequest request) {
        return commandClient.updateKycStatus(request);
    }

    public CustomerReadDto updateRiskScore(CustomerRiskUpdateRequest request) {
        return commandClient.updateRiskScore(request);
    }

    public CustomerReadDto updateMfaPreference(CustomerMfaUpdateRequest request) {
        return commandClient.updateMfaPreference(request);
    }

    public CustomerReadDto updateSpecialCustomer(CustomerSpecialUpdateRequest request) {
        return commandClient.updateSpecialCustomer(request);
    }

    public CustomerReadDto softDeleteCustomer(String id) {
        return commandClient.softDeleteCustomer(id);
    }

    public CustomerReadDto getById(String id) {
        return queryClient.getById(id);
    }

    public CustomerReadDto getByKeycloakId(String keycloakId) {
        return queryClient.getByKeycloakId(keycloakId);
    }

    public CustomerReadDto getByEmail(String email) {
        return queryClient.getByEmail(email);
    }

    public CustomerReadDto getByPhoneNumber(String phoneNumber) {
        return queryClient.getByPhoneNumber(phoneNumber);
    }

    public List<CustomerReadDto> search(String keyword, int limit) {
        return queryClient.search(keyword, limit);
    }

    public List<CustomerReadDto> listByStatus(String status, int limit) {
        return queryClient.listByStatus(status, limit);
    }

    public List<CustomerReadDto> listByKycStatus(String kycStatus, int limit) {
        return queryClient.listByKycStatus(kycStatus, limit);
    }

    public List<CustomerReadDto> listSpecialCustomers(int limit) {
        return queryClient.listSpecialCustomers(limit);
    }

    public List<CustomerReadDto> listHighRiskCustomers(int minRiskScore, int limit) {
        return queryClient.listHighRiskCustomers(minRiskScore, limit);
    }
}
