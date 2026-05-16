package com.banking_microservices.fraud_service.exception;

import com.banking_microservices.fraud_service.dto.ErrorResponseDto;
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
    void handleKafkaSendExceptionReturnsInternalServerError() {
        ResponseEntity<ErrorResponseDto> response = handler.handleKafkaSendException(
                new KafkaSendException("kafka down"), request("/fraud/check"));

        assertError(response, "KafkaSendException", "kafka down", "uri=/fraud/check");
    }

    @Test
    void handleRuntimeExceptionReturnsInternalServerError() {
        ResponseEntity<ErrorResponseDto> response = handler.handleRuntimeException(
                new RuntimeException("runtime"), request("/fraud/check"));

        assertError(response, "Internal Server Error", "runtime", "uri=/fraud/check");
    }

    private WebRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        return new ServletWebRequest(request);
    }

    private void assertError(ResponseEntity<ErrorResponseDto> response, String error, String message, String path) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals(error, response.getBody().getError());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(path, response.getBody().getPath());
    }
}
