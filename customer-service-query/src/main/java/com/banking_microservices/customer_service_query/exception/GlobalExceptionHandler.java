package com.banking_microservices.customer_service_query.exception;

import com.banking_microservices.customer_service_query.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReadModelNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ReadModelNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_READ_MODEL_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProjectionSyncException.class)
    public ResponseEntity<ErrorResponseDto> handleProjection(ProjectionSyncException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "CUSTOMER_PROJECTION_SYNC_FAILED", exception.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
