package com.banking_microservices.user_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.banking_microservices.user_service.dto.ErrorResponseDto;
import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MailNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleMailNotFoundException(MailNotFoundException e, WebRequest webRequest){
        log.warn("Mail Not Found. Error {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Mail Not Found")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException e, WebRequest webRequest){
        log.warn("User Already Exists {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("User Already Exists")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserSaveDatabaseException.class)
    public ResponseEntity<ErrorResponseDto> handleUserSaveDatabaseException(UserSaveDatabaseException e, WebRequest webRequest){
        log.warn("UserSaveDatabaseException {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("UserSaveDatabaseException")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CreateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleCreateUserException(CreateUserException e, WebRequest webRequest){
        log.warn("Entity CreateUserException. {} Path {}", e.getMessage(), webRequest.getDescription(true));
        ErrorResponseDto response = ErrorResponseDto
                .builder()
                .time(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("CreateUserException")
                .message(e.getMessage())
                .path(webRequest.getDescription(true))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e, WebRequest webRequest){
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



}

//package com.banking.user.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.context.request.WebRequest;
//import java.time.LocalDateTime;
//
//@RestControllerAdvice // Tüm controller'lardaki exception'ları yakalar
//public class GlobalExceptionHandler {
//
//    // UserAlreadyExistsException -> 400 Bad Request
//    @ExceptionHandler(UserAlreadyExistsException.class)
//    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
//        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "User Already Exists", ex.getMessage(), request.getDescription(false));
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    // UserNotFoundException -> 404 Not Found
//    @ExceptionHandler(UserNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
//        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "User Not Found", ex.getMessage(), request.getDescription(false));
//        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
//    }
//
//    // RuntimeException -> 400 Bad Request
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
//        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getDescription(false));
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    // Validation hatası -> 400 Bad Request
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
//        String message = ex.getBindingResult().getFieldError() != null ? ex.getBindingResult().getFieldError().getDefaultMessage() : "Validation error";
//        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Failed", message, request.getDescription(false));
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    // Genel hatalar -> 500 Internal Server Error
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
//        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(), request.getDescription(false));
//        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//}