package com.banking_microservices.user_service.exception;

public class UserNameOrSurnameNotFoundException extends RuntimeException {
    public UserNameOrSurnameNotFoundException(String message) {
        super(message);
    }
}
