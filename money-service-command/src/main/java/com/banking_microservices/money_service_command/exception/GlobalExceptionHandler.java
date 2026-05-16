package com.banking_microservices.money_service_command.exception;

import com.banking_microservices.money_service_command.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(AccountAlreadyExistsException exception) {
        return build(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(AccountNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({InsufficientFundsException.class, InvalidAmountException.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(MoneyCommandException exception) {
        return build(HttpStatus.BAD_REQUEST, "MONEY_COMMAND_VALIDATION", exception.getMessage());
    }

    @ExceptionHandler(ProjectionPublishException.class)
    public ResponseEntity<ErrorResponseDto> handleInfrastructure(Exception exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "MONEY_COMMAND_INFRA", exception.getMessage());
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
        return ResponseEntity.status(status)
                .body(new ErrorResponseDto(message, code, LocalDateTime.now()));
    }
}
