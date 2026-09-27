package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.address.AddressParserClient;
import com.banking_microservices.customer_service_command.address.ParsedAddress;
import com.banking_microservices.customer_service_command.dto.AddressInput;
import com.banking_microservices.customer_service_command.dto.enums.AddressParseStatus;
import com.banking_microservices.customer_service_command.dto.enums.AddressType;
import com.banking_microservices.customer_service_command.model.CustomerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerAddressService {
    private final AddressParserClient addressParserClient;

    public CustomerAddress parse(AddressInput input) {
        ParsedAddress parsed = addressParserClient.parse(input.rawAddress().trim(), input.countryCode());
        return CustomerAddress.builder()
                .type(input.type() == null ? AddressType.RESIDENTIAL : input.type())
                .rawAddress(input.rawAddress().trim())
                .country(parsed.country()).countryCode(firstNonBlank(parsed.countryCode(), input.countryCode()))
                .province(parsed.province()).district(parsed.district()).neighborhood(parsed.neighborhood())
                .road(parsed.road()).building(parsed.building()).buildingNumber(parsed.buildingNumber())
                .entrance(parsed.entrance()).floor(parsed.floor()).unit(parsed.unit()).postalCode(parsed.postalCode())
                .parseStatus(AddressParseStatus.PARSED).parser(parsed.parser()).parserVersion(parsed.parserVersion())
                .build();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
