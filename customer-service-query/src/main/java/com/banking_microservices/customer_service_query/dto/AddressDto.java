package com.banking_microservices.customer_service_query.dto;

import lombok.Data;

@Data
public class AddressDto {
    private String type;
    private String rawAddress;
    private String country;
    private String countryCode;
    private String province;
    private String district;
    private String neighborhood;
    private String road;
    private String building;
    private String buildingNumber;
    private String entrance;
    private String floor;
    private String unit;
    private String postalCode;
    private String parseStatus;
    private String parser;
    private String parserVersion;
}
