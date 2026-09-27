package com.banking_microservices.customer_service.exception;

import com.banking_microservices.customer_service.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerCommandClientException.class)
    public ResponseEntity<ErrorResponseDto> handleCommand(CustomerCommandClientException exception) {
        return build(HttpStatus.BAD_GATEWAY, "CUSTOMER_COMMAND_CLIENT_ERROR", exception.getMessage());
    }

    @ExceptionHandler(CustomerQueryClientException.class)
    public ResponseEntity<ErrorResponseDto> handleQuery(CustomerQueryClientException exception) {
        return build(HttpStatus.BAD_GATEWAY, "CUSTOMER_QUERY_CLIENT_ERROR", exception.getMessage());
    }

    @ExceptionHandler(CustomerOnboardingException.class)
    public ResponseEntity<ErrorResponseDto> handleOnboarding(CustomerOnboardingException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "CUSTOMER_ONBOARDING_ERROR", exception.getMessage());
    }

    @ExceptionHandler(CustomerAccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(CustomerAccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, "CUSTOMER_ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
