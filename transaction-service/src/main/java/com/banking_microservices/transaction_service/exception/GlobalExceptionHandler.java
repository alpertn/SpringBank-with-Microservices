package com.banking_microservices.transaction_service.exception;

import com.banking_microservices.transaction_service.dto.ErrorResponseDto;
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

    @ExceptionHandler(GetErrorLogsException.class)
    public ResponseEntity<ErrorResponseDto> handleGetErrorLogsException(GetErrorLogsException e, WebRequest webRequest) {
        log.error("GetErrorLogsException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("GetErrorLogsException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GetEventHistoryException.class)
    public ResponseEntity<ErrorResponseDto> handleGetEventHistoryException(GetEventHistoryException e, WebRequest webRequest) {
        log.error("GetEventHistoryException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("GetEventHistoryException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

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

    @ExceptionHandler(KafkaSendExceptionOnService.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendExceptionOnService(KafkaSendExceptionOnService e, WebRequest webRequest) {
        log.error("KafkaSendExceptionOnService. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KafkaSendExceptionOnService")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TransactionDtoSyntaxException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionDtoSyntaxException(TransactionDtoSyntaxException e, WebRequest webRequest) {
        log.warn("TransactionDtoSyntaxException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("TransactionDtoSyntaxException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionNotFoundException(TransactionNotFoundException e, WebRequest webRequest) {
        log.warn("TransactionNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("TransactionNotFoundException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionSaveException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionSaveException(TransactionSaveException e, WebRequest webRequest) {
        log.error("TransactionSaveException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("TransactionSaveException")
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
