package com.banking_microservices.admin_service_query.exception;

import com.banking_microservices.admin_service_query.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReadModelNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ReadModelNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "ADMIN_QUERY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(AdminQueryException.class)
    public ResponseEntity<ErrorResponseDto> handleQuery(AdminQueryException exception) {
        return build(HttpStatus.BAD_REQUEST, "ADMIN_QUERY_ERROR", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN_QUERY_INTERNAL", exception.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
