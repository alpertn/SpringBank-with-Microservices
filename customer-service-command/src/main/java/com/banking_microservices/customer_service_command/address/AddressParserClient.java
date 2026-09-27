package com.banking_microservices.customer_service_command.address;

public interface AddressParserClient {
    ParsedAddress parse(String rawAddress, String countryCode);
}
