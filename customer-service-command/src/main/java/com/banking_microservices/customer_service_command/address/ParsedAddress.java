package com.banking_microservices.customer_service_command.address;

public record ParsedAddress(
        String country,
        String countryCode,
        String province,
        String district,
        String neighborhood,
        String road,
        String building,
        String buildingNumber,
        String entrance,
        String floor,
        String unit,
        String postalCode,
        String parser,
        String parserVersion
) {
}
