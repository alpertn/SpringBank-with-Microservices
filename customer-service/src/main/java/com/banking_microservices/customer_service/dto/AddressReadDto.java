package com.banking_microservices.customer_service.dto;

public record AddressReadDto(
        String type, String rawAddress, String country, String countryCode, String province, String district,
        String neighborhood, String road, String building, String buildingNumber, String entrance, String floor,
        String unit, String postalCode, String parseStatus, String parser, String parserVersion
) {}
