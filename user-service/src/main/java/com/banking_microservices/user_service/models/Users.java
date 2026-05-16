package com.banking_microservices.user_service.models;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    private String id;
    private String keycloakUUID;
    private String mail;
    private String password;
    private String name;
    private String surname;
    @Builder.Default
    private Role role = Role.USER;
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
}
