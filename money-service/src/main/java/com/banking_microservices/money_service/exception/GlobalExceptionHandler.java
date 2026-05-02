package com.banking_microservices.money_service.exception;

import com.banking_microservices.money_service.dto.ErrorResponseDto;
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

    @ExceptionHandler(DepositFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleDepositFailedException(DepositFailedException e, WebRequest webRequest) {
        log.warn("DepositFailedException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Deposit Failed", e.getMessage(), webRequest);
    }

    @ExceptionHandler(IbanNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleIbanNotFoundException(IbanNotFoundException e, WebRequest webRequest) {
        log.warn("IbanNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "Iban Not Found", e.getMessage(), webRequest);
    }

    @ExceptionHandler(GenerateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleGenerateUserException(GenerateUserException e, WebRequest webRequest) {
        log.warn("GenerateUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Cannot Generate User Entity", e.getMessage(), webRequest);
    }

    @ExceptionHandler(MoneyNotAvaibleException.class)
    public ResponseEntity<ErrorResponseDto> handleMoneyNotAvaibleException(MoneyNotAvaibleException e, WebRequest webRequest) {
        log.warn("MoneyNotAvaibleException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Insufficient Funds", e.getMessage(), webRequest);
    }

    @ExceptionHandler(SaveUserException.class)
    public ResponseEntity<ErrorResponseDto> handleSaveUserException(SaveUserException e, WebRequest webRequest) {
        log.warn("SaveUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "User Save Failed", e.getMessage(), webRequest);
    }

    @ExceptionHandler(NegativeNumberException.class)
    public ResponseEntity<ErrorResponseDto> handleNegativeNumberException(NegativeNumberException e, WebRequest webRequest) {
        log.warn("NegativeNumberException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Negative Number Detected", e.getMessage(), webRequest);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(UserNotFoundException e, WebRequest webRequest) {
        log.warn("UserNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "User Not Found", e.getMessage(), webRequest);
    }

    @ExceptionHandler(DecramentAndBlockMoneyException.class)
    public ResponseEntity<ErrorResponseDto> handleDecramentAndBlockMoneyException(DecramentAndBlockMoneyException e, WebRequest webRequest) {
        log.warn("DecramentAndBlockMoneyException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "DecramentAndBlockMoneyException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(DeposItOrWithdrawFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleDeposItOrWithdrawFailedException(DeposItOrWithdrawFailedException e, WebRequest webRequest) {
        log.warn("DeposItOrWithdrawFailedException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Deposit Or Withdraw Failed", e.getMessage(), webRequest);
    }

    @ExceptionHandler(EventSaveException.class)
    public ResponseEntity<ErrorResponseDto> handleEventSaveException(EventSaveException e, WebRequest webRequest) {
        log.error("EventSaveException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "EventSaveException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(EventUUIDAlreadyExists.class)
    public ResponseEntity<ErrorResponseDto> handleEventUUIDAlreadyExists(EventUUIDAlreadyExists e, WebRequest webRequest) {
        log.warn("EventUUIDAlreadyExists. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.CONFLICT, "Event UUID Already Exists", e.getMessage(), webRequest);
    }

    @ExceptionHandler(EventUUIDNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEventUUIDNotFoundException(EventUUIDNotFoundException e, WebRequest webRequest) {
        log.warn("EventUUIDNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "Event UUID Not Found", e.getMessage(), webRequest);
    }

    @ExceptionHandler(KafkaCreateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaCreateUserException(KafkaCreateUserException e, WebRequest webRequest) {
        log.error("KafkaCreateUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "KafkaCreateUserException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(KafkaSendException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaSendException(KafkaSendException e, WebRequest webRequest) {
        log.error("KafkaSendException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "KafkaSendException", e.getMessage(), webRequest);
    }

    @ExceptionHandler(SameAccountException.class)
    public ResponseEntity<ErrorResponseDto> handleSameAccountException(SameAccountException e, WebRequest webRequest) {
        log.warn("SameAccountException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.BAD_REQUEST, "Same Account Transfer Not Allowed", e.getMessage(), webRequest);
    }

    // ─── Saga Exception'ları ──────────────────────────────────────────────────

    @ExceptionHandler(SagaEventNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleSagaEventNotFoundException(SagaEventNotFoundException e, WebRequest webRequest) {
        log.warn("SagaEventNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.NOT_FOUND, "Saga Event Not Found", e.getMessage(), webRequest);
    }

    @ExceptionHandler(SagaTransactionRollbackException.class)
    public ResponseEntity<ErrorResponseDto> handleSagaTransactionRollbackException(SagaTransactionRollbackException e, WebRequest webRequest) {
        log.error("SagaTransactionRollbackException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Saga Rollback Failed", e.getMessage(), webRequest);
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