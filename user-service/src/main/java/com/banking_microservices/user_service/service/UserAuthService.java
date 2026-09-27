package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.auth.LoginRequestDto;
import com.banking_microservices.user_service.dto.auth.RefleshTokenRequestDto;
import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.dto.auth.TokenResponseDto;
import com.banking_microservices.user_service.exception.CustomerOnboardingException;
import com.banking_microservices.user_service.exception.CustomerRollbackException;
import com.banking_microservices.user_service.grpc.CustomerOnboardingGrpcClient;
import com.banking_microservices.user_service.kafka.KafkaSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserAuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;
    private final KafkaSender kafkaSender;
    private final CustomerOnboardingGrpcClient customerOnboardingGrpcClient;

    public UserAuthService(KeycloakAdminService keycloakAdminService,
                           KeycloakUserService keycloakUserService,
                           KafkaSender kafkaSender,
                           CustomerOnboardingGrpcClient customerOnboardingGrpcClient) {
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakUserService = keycloakUserService;
        this.kafkaSender = kafkaSender;
        this.customerOnboardingGrpcClient = customerOnboardingGrpcClient;
    }

    public void register(RegisterDto dto) {
        String keycloakUserId = keycloakAdminService.createUser(dto, Role.USER);
        try {
            String customerId = customerOnboardingGrpcClient.createCustomerForUser(keycloakUserId, dto);
            log.info("UserAuthService | register -> Customer onboarding completed. keycloakUserId={}, customerId={}",
                    keycloakUserId, customerId);
            kafkaSender.sendCreateUser(keycloakUserId);
        } catch (CustomerOnboardingException exception) {
            rollbackKeycloakUser(keycloakUserId, exception);
            throw exception;
        } catch (Exception exception) {
            CustomerOnboardingException wrapped = new CustomerOnboardingException("Unexpected customer onboarding failure", exception);
            rollbackKeycloakUser(keycloakUserId, wrapped);
            throw wrapped;
        }
    }

    private void rollbackKeycloakUser(String keycloakUserId, RuntimeException originalException) {
        try {
            keycloakAdminService.deleteUserById(keycloakUserId);
            log.warn("UserAuthService | register -> Keycloak rollback completed. keycloakUserId={}", keycloakUserId);
        } catch (Exception rollbackException) {
            CustomerRollbackException rollbackFailure = new CustomerRollbackException("Keycloak rollback failed for keycloakUserId=" + keycloakUserId, rollbackException);
            originalException.addSuppressed(rollbackFailure);
            log.error("UserAuthService | register -> Keycloak rollback failed. keycloakUserId={}, error={}",
                    keycloakUserId, rollbackException.getMessage());
        }
    }

    public TokenResponseDto login(LoginRequestDto dto) {
        return keycloakUserService.login(dto);
    }

    public TokenResponseDto refresh(RefleshTokenRequestDto dto) {
        return keycloakUserService.refreshWithRefreshToken(dto.getRefreshToken());
    }

    public void logout(RefleshTokenRequestDto dto) {
        keycloakUserService.logout(dto.getRefreshToken());
    }
}
