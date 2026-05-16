package com.banking_microservices.money_service_query.exception;

import com.banking_microservices.money_service_query.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturnsNotFoundErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleNotFound(new ReadModelNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("READ_MODEL_NOT_FOUND", response.getBody().errorCode());
        assertEquals("missing", response.getBody().message());
    }

    @Test
    void handleFailureReturnsServiceUnavailableErrorResponse() {
        ResponseEntity<ErrorResponseDto> response = handler.handleFailure(
                new ProjectionSyncException("sync failed", new RuntimeException("elasticsearch")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MONEY_QUERY_FAILURE", response.getBody().errorCode());
        assertEquals("sync failed", response.getBody().message());
    }
}
