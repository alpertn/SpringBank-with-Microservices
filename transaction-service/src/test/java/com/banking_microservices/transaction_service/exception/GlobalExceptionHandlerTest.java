package com.banking_microservices.transaction_service.exception;

import com.banking_microservices.transaction_service.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTransactionDtoSyntaxExceptionReturnsBadRequest() {
        ResponseEntity<ErrorResponseDto> response = handler.handleTransactionDtoSyntaxException(
                new TransactionDtoSyntaxException("bad dto"), request("/transactions"));

        assertError(response, HttpStatus.BAD_REQUEST, "TransactionDtoSyntaxException", "bad dto", "uri=/transactions");
    }

    @Test
    void handleTransactionNotFoundExceptionReturnsNotFound() {
        ResponseEntity<ErrorResponseDto> response = handler.handleTransactionNotFoundException(
                new TransactionNotFoundException("not found"), request("/transactions/42"));

        assertError(response, HttpStatus.NOT_FOUND, "TransactionNotFoundException", "not found", "uri=/transactions/42");
    }

    @Test
    void handleValueNotFoundExceptionReturnsBadRequest() {
        ResponseEntity<ErrorResponseDto> response = handler.handleValueNotFoundException(
                new ValueNotFoundException("missing value"), request("/transactions"));

        assertError(response, HttpStatus.BAD_REQUEST, "ValueNotFoundException", "missing value", "uri=/transactions");
    }

    @Test
    void handleGlobalExceptionReturnsInternalServerError() {
        ResponseEntity<ErrorResponseDto> response = handler.handleGlobalException(
                new Exception("unexpected"), request("/transactions"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "unexpected", "uri=/transactions");
    }

    private WebRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        return new ServletWebRequest(request);
    }

    private void assertError(ResponseEntity<ErrorResponseDto> response, HttpStatus status, String error,
                             String message, String path) {
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(status.value(), response.getBody().getStatus());
        assertEquals(error, response.getBody().getError());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(path, response.getBody().getPath());
    }
}
