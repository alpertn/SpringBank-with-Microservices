package com.banking_microservices.user_service.dto.user;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthServiceCreateUserTopicDto {
    private String keycloakUserUUID;
    private String email;
    private String name;
    private String surname;
    private String password;
    private Role role;
}
