package com.banking_microservices.admin_service_command.exception;

public class AdminCommandException extends RuntimeException {

    public AdminCommandException(String message) {
        super(message);
    }

    public AdminCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
