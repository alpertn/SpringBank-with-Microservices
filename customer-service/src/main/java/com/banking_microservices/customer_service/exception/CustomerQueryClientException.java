package com.banking_microservices.customer_service.exception;

public class CustomerQueryClientException extends CustomerServiceException {

    public CustomerQueryClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
