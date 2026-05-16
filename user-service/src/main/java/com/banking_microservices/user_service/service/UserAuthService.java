package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.auth.LoginRequestDto;
import com.banking_microservices.user_service.dto.auth.RefleshTokenRequestDto;
import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.dto.auth.TokenResponseDto;
import com.banking_microservices.user_service.kafka.KafkaSender;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;
    private final KafkaSender kafkaSender;

    public UserAuthService(KeycloakAdminService keycloakAdminService,
                           KeycloakUserService keycloakUserService,
                           KafkaSender kafkaSender) {
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakUserService = keycloakUserService;
        this.kafkaSender = kafkaSender;
    }

    public void register(RegisterDto dto) {
        String keycloakUserId = keycloakAdminService.createUser(dto, Role.USER);
        kafkaSender.sendCreateUser(keycloakUserId);
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
