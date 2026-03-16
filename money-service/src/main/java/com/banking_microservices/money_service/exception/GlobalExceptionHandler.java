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
    public ResponseEntity<ErrorResponseDto> handleDepositFailedException(DepositFailedException e, WebRequest webRequest){
        log.warn("DepositFailedException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Deposit Failed")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IbanNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleIbanNotFoundException(IbanNotFoundException e, WebRequest webRequest){
        log.warn("IbanNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Iban not found")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(GenerateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleGenerateUserException(GenerateUserException e, WebRequest webRequest){
        log.warn("GenerateUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Can not generated user entity")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MoneyNotAvaibleException.class)
    public ResponseEntity<ErrorResponseDto> handleMoneyNotAvaibleException(MoneyNotAvaibleException e, WebRequest webRequest){
        log.warn("UserMoney Not Avaible. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("UserMoney not avaible")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(SaveUserException.class)
    public ResponseEntity<ErrorResponseDto> handleSaveUserException(SaveUserException e, WebRequest webRequest){
        log.warn("SaveUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("User Save Failed")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NegativeNumberException.class)
    public ResponseEntity<ErrorResponseDto> handleNegativeNumberException(NegativeNumberException e, WebRequest webRequest){
        log.warn("NegativeNumberException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Negative Number Detected")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(UserNotFoundException e, WebRequest webRequest){
        log.warn("UserNotFoundException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(DecramentAndBlockMoneyException.class)
    public ResponseEntity<ErrorResponseDto> handleDecramentAndBlockMoneyException(DecramentAndBlockMoneyException e, WebRequest webRequest) {
        log.warn("DecramentAndBlockMoneyException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("DecramentAndBlockMoneyException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeposItOrWithdrawFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleDeposItOrWithdrawFailedException(DeposItOrWithdrawFailedException e, WebRequest webRequest) {
        log.warn("DeposItOrWithdrawFailedException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("DeposItOrWithdrawFailedException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EventSaveException.class)
    public ResponseEntity<ErrorResponseDto> handleEventSaveException(EventSaveException e, WebRequest webRequest) {
        log.error("EventSaveException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("EventSaveException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EventUUIDAlreadyExists.class)
    public ResponseEntity<ErrorResponseDto> handleEventUUIDAlreadyExists(EventUUIDAlreadyExists e, WebRequest webRequest) {
        log.warn("EventUUIDAlreadyExists. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("EventUUIDAlreadyExists")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(KafkaCreateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleKafkaCreateUserException(KafkaCreateUserException e, WebRequest webRequest) {
        log.error("KafkaCreateUserException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("KafkaCreateUserException")
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

    @ExceptionHandler(SameAccountException.class)
    public ResponseEntity<ErrorResponseDto> handleSameAccountException(SameAccountException e, WebRequest webRequest) {
        log.warn("SameAccountException. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("SameAccountException")
                .message(e.getMessage())
                .path(webRequest.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
}