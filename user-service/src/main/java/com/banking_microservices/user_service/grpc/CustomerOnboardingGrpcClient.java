package com.banking_microservices.user_service.grpc;

import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.exception.CustomerOnboardingException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerOnboardingGrpcClient {

    @GrpcClient("customer-onboarding")
    private CustomerOnboardingGrpcServiceGrpc.CustomerOnboardingGrpcServiceBlockingStub stub;

    public String createCustomerForUser(String keycloakUserId, RegisterDto dto) {
        try {
            CustomerOnboardingCreateResponse response = stub.createCustomerForUser(CustomerOnboardingCreateRequest.newBuilder()
                    .setKeycloakId(value(keycloakUserId))
                    .setEmail(value(dto.getEmail()))
                    .setEmailVerified(true)
                    .setPhoneNumber(value(dto.getPhoneNumber()))
                    .setName(value(dto.getName()))
                    .setSurname(value(dto.getSurname()))
                    .setBirthdate(value(dto.getBirthdate()))
                    .setNationalId(value(dto.getNationalId()))
                    .setNationalityCode(value(dto.getNationalityCode()))
                    .setMaritalStatus(value(dto.getMaritalStatus()))
                    .setRawAddress(value(dto.getRawAddress()))
                    .setAddressCountryCode(value(dto.getAddressCountryCode()))
                    .build());
            return response.getCustomerId();
        } catch (StatusRuntimeException exception) {
            throw new CustomerOnboardingException("customer-service onboarding failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
