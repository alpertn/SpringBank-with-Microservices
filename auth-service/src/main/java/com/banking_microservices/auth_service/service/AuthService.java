package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.CreateUserTopicDto;
import com.banking_microservices.auth_service.dto.RegisterDto;
import com.banking_microservices.auth_service.dto.Role;
import com.banking_microservices.auth_service.exception.KeycloackUserCreateException;
import com.banking_microservices.auth_service.kafka.KafkaSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final KeycloackAdminService keycloackAdminService;
    private final KeycloackUserService keycloackUserService;
    private final KafkaSender kafkaSender;

    public void createUser(RegisterDto dto) {

        try {
            String keycloackUserUUID = keycloackAdminService.createKeycloakUser(dto, Role.USER);

            CreateUserTopicDto userTopicDto = CreateUserTopicDto.builder()
                    .keycloackUserUUID(keycloackUserUUID)
                    .name(dto.getName())
                    .surname(dto.getSurname())
                    .password(dto.getPassword())
                    .email(dto.getEmail()).build();

            kafkaSender.sendCreateUserToUserTopic(userTopicDto);

        } catch (Exception e) {
            log.error("Failed to create Keycloak user", e);
            throw new KeycloackUserCreateException("An exception with Keycloack create user: " + e.getMessage());
        }

    }

}
