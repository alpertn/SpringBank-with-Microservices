package com.banking_microservices.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();
    private int status;
    private int error;
    private String errorMessage;
}
