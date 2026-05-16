package com.banking_microservices.money_service_command.exception;

public class ProjectionPublishException extends MoneyCommandException {

    public ProjectionPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
