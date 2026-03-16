package com.banking_microservices.auth_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.banking_microservices.auth_service.dto.ErrorResponseDto;
import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<ErrorResponseDto> handleLoginException(LoginException e, WebRequest webRequest) {
        log.warn("LoginException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Login Failed")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidTokenException(InvalidTokenException e,
            WebRequest webRequest) {
        log.warn("InvalidTokenException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Invalid Token")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LogoutException.class)
    public ResponseEntity<ErrorResponseDto> handleLogoutException(LogoutException e, WebRequest webRequest) {
        log.warn("LogoutException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Logout Failed")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponseDto> handleTokenRefreshException(TokenRefreshException e,
            WebRequest webRequest) {
        log.warn("TokenRefreshException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Token Refresh Failed")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(KeycloakConnectionException.class)
    public ResponseEntity<ErrorResponseDto> handleKeycloakConnectionException(KeycloakConnectionException e,
            WebRequest webRequest) {
        log.warn("KeycloakConnectionException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Keycloak Connection Error")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e, WebRequest webRequest) {
        log.warn("RuntimeException Error. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Runtime Exception")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception e, WebRequest webRequest) {
        log.error("Unhandled Exception. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(KeycloakAssignRoleException.class)
    public ResponseEntity<ErrorResponseDto> handleKeycloakAssignRoleException(KeycloakAssignRoleException e, WebRequest webRequest) {
        log.error("Unhandled Exception. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KeycloakAssignRoleException")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(KeycloakUserAlreadyExists.class)
    public ResponseEntity<ErrorResponseDto> handleKeycloakUserAlreadyExists(KeycloakUserAlreadyExists e, WebRequest webRequest) {
        log.error("Unhandled Exception. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KeycloakUserAlreadyExists")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(KeycloackUserCreateException.class)
    public ResponseEntity<ErrorResponseDto> handleKeycloackUserCreateException(KeycloackUserCreateException e, WebRequest webRequest) {
        log.error("KeycloackUserCreateException. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KeycloackUserCreateException")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(KafkaSendException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendException(KafkaSendException e, WebRequest webRequest) {
        log.error("KafkaSendException. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KafkaSendException")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
