package com.banking_microservices.user_service.dto.admin;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersDto {
    private String id;
    private String mail;
    private String password;
    private String name;
    private String surname;
    private String role;
}
