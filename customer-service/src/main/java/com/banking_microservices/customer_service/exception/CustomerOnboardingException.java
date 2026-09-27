package com.banking_microservices.customer_service.exception;

public class CustomerOnboardingException extends CustomerServiceException {

    public CustomerOnboardingException(String message, Throwable cause) {
        super(message, cause);
    }
}
