package com.banking_microservices.customer_service_command.exception;

public class AddressParsingException extends CustomerCommandException {
    public AddressParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AddressParsingException(String message) {
        super(message);
    }
}
