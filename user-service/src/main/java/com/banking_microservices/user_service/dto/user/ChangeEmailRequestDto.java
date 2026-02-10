package com.banking_microservices.user_service.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequestDto {
    @NotBlank(message = "New email is required")
    @Email(message = "Email must be valid")
    private String newEmail;

    @NotBlank(message = "Password is required for verification")
    private String password;
}
