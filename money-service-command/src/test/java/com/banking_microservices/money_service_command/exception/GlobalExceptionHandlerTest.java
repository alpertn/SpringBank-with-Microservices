package com.banking_microservices.money_service_command.exception;

import com.banking_microservices.money_service_command.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConflictReturnsConflictErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleConflict(new AccountAlreadyExistsException("exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCOUNT_ALREADY_EXISTS", response.getBody().errorCode());
        assertEquals("exists", response.getBody().message());
    }

    @Test
    void handleNotFoundReturnsNotFoundErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleNotFound(new AccountNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCOUNT_NOT_FOUND", response.getBody().errorCode());
        assertEquals("missing", response.getBody().message());
    }

    @Test
    void handleBadRequestReturnsValidationErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleBadRequest(new InvalidAmountException("invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MONEY_COMMAND_VALIDATION", response.getBody().errorCode());
        assertEquals("invalid", response.getBody().message());
    }

    @Test
    void handleInfrastructureReturnsServiceUnavailableErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleInfrastructure(
                new ProjectionPublishException("publish failed", new RuntimeException("broker down")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MONEY_COMMAND_INFRA", response.getBody().errorCode());
        assertEquals("publish failed", response.getBody().message());
    }
}
