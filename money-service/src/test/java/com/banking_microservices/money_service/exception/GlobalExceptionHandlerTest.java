package com.banking_microservices.money_service.exception;

import com.banking_microservices.money_service.dto.ErrorResponseDto;
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
    void handleDepositFailedExceptionReturnsBadRequest() {
        ResponseEntity<ErrorResponseDto> response = handler.handleDepositFailedException(
                new DepositFailedException("deposit failed"), request("/money/deposit"));

        assertError(response, HttpStatus.BAD_REQUEST, "Deposit Failed", "deposit failed", "uri=/money/deposit");
    }

    @Test
    void handleIbanNotFoundExceptionReturnsNotFound() {
        ResponseEntity<ErrorResponseDto> response = handler.handleIbanNotFoundException(
                new IbanNotFoundException("iban missing"), request("/money/iban"));

        assertError(response, HttpStatus.NOT_FOUND, "Iban Not Found", "iban missing", "uri=/money/iban");
    }

    @Test
    void handleSameAccountExceptionReturnsBadRequest() {
        ResponseEntity<ErrorResponseDto> response = handler.handleSameAccountException(
                new SameAccountException("same account"), request("/money/transfer"));

        assertError(response, HttpStatus.BAD_REQUEST, "Same Account Transfer Not Allowed", "same account",
                "uri=/money/transfer");
    }

    @Test
    void handleGlobalExceptionReturnsInternalServerError() {
        ResponseEntity<ErrorResponseDto> response = handler.handleGlobalException(
                new Exception("unexpected"), request("/money"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "unexpected", "uri=/money");
    }

    private WebRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
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
