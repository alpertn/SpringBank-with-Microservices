package com.banking_microservices.customer_service_command.address;

record LibpostalParseResponse(
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
    ParsedAddress toParsedAddress() {
        return new ParsedAddress(country, countryCode, province, district, neighborhood, road, building,
                buildingNumber, entrance, floor, unit, postalCode, parser, parserVersion);
    }
}
