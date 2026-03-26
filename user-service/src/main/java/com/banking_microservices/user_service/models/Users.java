package com.banking_microservices.user_service.models;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @UuidGenerator
    private String id;
    private String keycloackUUID;
    private String mail;
    private String password;
    private String name;
    private String surname;
    @Builder.Default
    private Role role = Role.USER;
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
