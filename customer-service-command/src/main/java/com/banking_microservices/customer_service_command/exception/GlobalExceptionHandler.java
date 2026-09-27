package com.banking_microservices.customer_service_command.exception;

import com.banking_microservices.customer_service_command.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(CustomerAlreadyExistsException exception) {
        return build(HttpStatus.CONFLICT, "CUSTOMER_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(CustomerNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({
            InvalidCustomerStateException.class,
            InvalidKycTransitionException.class,
            InvalidRiskScoreException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(CustomerCommandException exception) {
        return build(HttpStatus.BAD_REQUEST, "CUSTOMER_COMMAND_VALIDATION", exception.getMessage());
    }

    @ExceptionHandler(ProjectionPublishException.class)
    public ResponseEntity<ErrorResponseDto> handleInfrastructure(ProjectionPublishException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "CUSTOMER_COMMAND_INFRA", exception.getMessage());
    }

    @ExceptionHandler(AddressParsingException.class)
    public ResponseEntity<ErrorResponseDto> handleAddressParser(AddressParsingException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "ADDRESS_PARSER_UNAVAILABLE", exception.getMessage());
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
