package com.banking_microservices.money_service.exception;

public class DeposItOrWithdrawFailedException extends RuntimeException {
    public DeposItOrWithdrawFailedException(String message) {
        super(message);
    }
}
