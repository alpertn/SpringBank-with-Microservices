package com.banking_microservices.user_service.models;

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
    @Builder.Default
    private String name = "Ahmet";
    @Builder.Default
    private String surname = "Kar";
    @Builder.Default
    private String role = "USER";
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
