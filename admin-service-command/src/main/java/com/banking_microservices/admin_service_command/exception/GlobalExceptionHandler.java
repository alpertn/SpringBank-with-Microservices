package com.banking_microservices.admin_service_command.exception;

import com.banking_microservices.admin_service_command.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AdminCommandException.class)
    public ResponseEntity<ErrorResponseDto> handleAdminCommand(AdminCommandException exception) {
        return build(HttpStatus.BAD_REQUEST, "ADMIN_COMMAND_ERROR", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN_COMMAND_INTERNAL", exception.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
