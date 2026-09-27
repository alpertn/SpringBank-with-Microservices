package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.exception.InvalidCustomerStateException;

public final class CustomerEnumParser {

    private CustomerEnumParser() {
    }

    public static <T extends Enum<T>> T parse(Class<T> enumType, String value, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidCustomerStateException("Invalid " + enumType.getSimpleName() + " value: " + value);
        }
    }
}
