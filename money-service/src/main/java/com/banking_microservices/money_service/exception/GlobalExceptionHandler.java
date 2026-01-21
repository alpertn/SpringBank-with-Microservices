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
        log.warn("Money Not Avaible. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Money not avaible")
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