package com.banking_microservices.user_service.exception;

import com.banking_microservices.user_service.dto.user.ErrorResponseDto;
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
    void handleMailNotFoundExceptionReturnsNotFound() {
        ResponseEntity<ErrorResponseDto> response = handler.handleMailNotFoundException(
                new MailNotFoundException("mail missing"), request("/auth/login"));

        assertError(response, HttpStatus.NOT_FOUND, "Mail Not Found", "mail missing", "uri=/auth/login;client=127.0.0.1");
    }

    @Test
    void handleUserAlreadyExistsExceptionReturnsConflict() {
        ResponseEntity<ErrorResponseDto> response = handler.handleUserAlreadyExistsException(
                new UserAlreadyExistsException("exists"), request("/auth/register"));

        assertError(response, HttpStatus.CONFLICT, "User Already Exists", "exists", "uri=/auth/register;client=127.0.0.1");
    }

    @Test
    void handleLoginExceptionReturnsUnauthorized() {
        ResponseEntity<ErrorResponseDto> response = handler.handleLoginException(
                new LoginException("bad credentials"), request("/auth/login"));

        assertError(response, HttpStatus.UNAUTHORIZED, "Login Failed", "bad credentials", "uri=/auth/login;client=127.0.0.1");
    }

    @Test
    void handleInvalidTokenExceptionReturnsUnauthorized() {
        ResponseEntity<ErrorResponseDto> response = handler.handleInvalidTokenException(
                new InvalidTokenException("invalid token"), request("/auth/refresh"));

        assertError(response, HttpStatus.UNAUTHORIZED, "Invalid Token", "invalid token", "uri=/auth/refresh;client=127.0.0.1");
    }

    @Test
    void handleKeycloakConnectionExceptionReturnsServiceUnavailable() {
        ResponseEntity<ErrorResponseDto> response = handler.handleKeycloakConnectionException(
                new KeycloakConnectionException("keycloak down"), request("/auth/register"));

        assertError(response, HttpStatus.SERVICE_UNAVAILABLE, "Keycloak Connection Error", "keycloak down",
                "uri=/auth/register;client=127.0.0.1");
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
