package com.banking_microservices.fraud_service.exception;

import com.banking_microservices.fraud_service.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(KafkaSendException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendException(KafkaSendException e, WebRequest webRequest) {
        log.error("KafkaSendException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KafkaSendException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e, WebRequest webRequest){
        log.warn("RuntimeException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true), e);
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception e, WebRequest webRequest) {
        log.error("Unhandled Exception. {} Path {}", e.getMessage(), webRequest.getDescription(true), e);
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
}
