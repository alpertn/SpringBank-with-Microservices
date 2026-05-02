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
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "GetErrorLogsException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(GetEventHistoryException.class)
    public ResponseEntity<ErrorResponseDto> handleGetEventHistoryException(GetEventHistoryException e, WebRequest webRequest) {
        log.error("GetEventHistoryException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "GetEventHistoryException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(KafkaSendException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendException(KafkaSendException e, WebRequest webRequest) {
        log.error("KafkaSendException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "KafkaSendException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(KafkaSendExceptionOnService.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendExceptionOnService(KafkaSendExceptionOnService e, WebRequest webRequest) {
        log.error("KafkaSendExceptionOnService. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "KafkaSendExceptionOnService", e.getMessage(), webRequest);
    }

    @ExceptionHandler(TransactionDtoSyntaxException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionDtoSyntaxException(TransactionDtoSyntaxException e, WebRequest webRequest) {
        log.warn("TransactionDtoSyntaxException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "TransactionDtoSyntaxException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionNotFoundException(TransactionNotFoundException e, WebRequest webRequest) {
        log.warn("TransactionNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "TransactionNotFoundException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(TransactionSaveException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionSaveException(TransactionSaveException e, WebRequest webRequest) {
        log.error("TransactionSaveException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "TransactionSaveException", e.getMessage(), webRequest);
    }

    // ─── Saga Exception'ları ──────────────────────────────────────────────────

    @ExceptionHandler(SagaEventNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleSagaEventNotFoundException(SagaEventNotFoundException e, WebRequest webRequest) {
        log.warn("SagaEventNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "SagaEventNotFoundException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(SagaEventDatabaseSaveException.class)
    public ResponseEntity<ErrorResponseDto> handleSagaEventDatabaseSaveException(SagaEventDatabaseSaveException e, WebRequest webRequest) {
        log.error("SagaEventDatabaseSaveException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "SagaEventDatabaseSaveException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(ValueNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleValueNotFoundException(ValueNotFoundException e, WebRequest webRequest) {
        log.warn("ValueNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "ValueNotFoundException", e.getMessage(), webRequest);
    }

    // ─── Genel Fallback'ler ───────────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e, WebRequest webRequest) {
        log.warn("RuntimeException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), webRequest);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception e, WebRequest webRequest) {
        log.error("Unhandled Exception. {} Path {}", e.getMessage(), webRequest.getDescription(true), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), webRequest);
    }

    // ─── Yardımcı Builder ────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String error, String message, WebRequest req) {
        ErrorResponseDto response = ErrorResponseDto.builder()
                .time(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(req.getDescription(false))
                .build();
        return new ResponseEntity<>(response, status);
    }
}
