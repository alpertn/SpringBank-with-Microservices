package com.banking_microservices.money_service_query.exception;

import com.banking_microservices.money_service_query.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReadModelNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ReadModelNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "READ_MODEL_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({ProjectionSyncException.class, MoneyQueryException.class})
    public ResponseEntity<ErrorResponseDto> handleFailure(MoneyQueryException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "MONEY_QUERY_FAILURE", exception.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
