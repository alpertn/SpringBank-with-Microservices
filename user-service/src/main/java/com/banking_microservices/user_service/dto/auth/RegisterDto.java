package com.banking_microservices.user_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    @NotBlank
    private String password;
    private String phoneNumber;
    private String birthdate;
    @NotBlank @Pattern(regexp = "[1-9][0-9]{10}")
    private String nationalId;
    @NotBlank @Size(min = 2, max = 3)
    private String nationalityCode;
    private String maritalStatus;
    @NotBlank @Size(max = 1000)
    private String rawAddress;
    @NotBlank @Size(min = 2, max = 3)
    private String addressCountryCode;
}
