package com.banking_microservices.user_service.exception;

public class OpenFeignMoneyServiceClientException extends RuntimeException {
    public OpenFeignMoneyServiceClientException(String message) {
        super(message);
    }
}
