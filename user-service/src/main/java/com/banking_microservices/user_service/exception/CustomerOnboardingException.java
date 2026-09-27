package com.banking_microservices.user_service.exception;

public class CustomerOnboardingException extends RuntimeException {

    public CustomerOnboardingException(String message, Throwable cause) {
        super(message, cause);
    }
}
