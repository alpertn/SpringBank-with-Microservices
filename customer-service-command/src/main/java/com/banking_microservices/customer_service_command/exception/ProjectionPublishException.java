package com.banking_microservices.customer_service_command.exception;

public class ProjectionPublishException extends CustomerCommandException {
    public ProjectionPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
