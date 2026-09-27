package com.banking_microservices.customer_service_command.model;

import com.banking_microservices.customer_service_command.dto.enums.AddressParseStatus;
import com.banking_microservices.customer_service_command.dto.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 30)
    private AddressType type;

    @Column(name = "address_raw", length = 1000)
    private String rawAddress;
    @Column(name = "address_country", length = 100)
    private String country;
    @Column(name = "address_country_code", length = 3)
    private String countryCode;
    @Column(name = "address_province", length = 100)
    private String province;
    @Column(name = "address_district", length = 100)
    private String district;
    @Column(name = "address_neighborhood", length = 150)
    private String neighborhood;
    @Column(name = "address_road", length = 200)
    private String road;
    @Column(name = "address_building", length = 150)
    private String building;
    @Column(name = "address_building_number", length = 30)
    private String buildingNumber;
    @Column(name = "address_entrance", length = 30)
    private String entrance;
    @Column(name = "address_floor", length = 30)
    private String floor;
    @Column(name = "address_unit", length = 30)
    private String unit;
    @Column(name = "address_postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_parse_status", length = 30)
    private AddressParseStatus parseStatus;
    @Column(name = "address_parser", length = 50)
    private String parser;
    @Column(name = "address_parser_version", length = 80)
    private String parserVersion;
}
