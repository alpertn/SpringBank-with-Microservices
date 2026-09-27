package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.CustomerResponseDto;
import com.banking_microservices.customer_service_command.dto.AddressDto;
import com.banking_microservices.customer_service_command.model.Customer;
import com.banking_microservices.customer_service_command.model.CustomerAddress;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponseDto toResponse(Customer customer) {
        return new CustomerResponseDto(
                customer.getId(),
                customer.getKeycloakId(),
                customer.getRealm().name(),
                customer.getEmail(),
                customer.isEmailVerified(),
                customer.getPhoneNumber(),
                customer.isPhoneVerified(),
                customer.getUserType().name(),
                customer.getStatus().name(),
                customer.getBirthdate(),
                customer.getName(),
                customer.getMiddleName(),
                customer.getSurname(),
                customer.getSex().name(),
                customer.getRiskScore(),
                customer.isMfaEnabled(),
                customer.getMfaMethod().name(),
                customer.getPreferredLanguage(),
                customer.isSpecialCustomer(),
                customer.getSpecialCustomerScore(),
                customer.getKycStatus().name(),
                customer.getNationalityCode().name(),
                customer.getNationalIdMasked(),
                customer.getAccountStatus().name(),
                customer.getMaritalStatus().name(),
                customer.getRiskClass().name(),
                customer.getSegment().name(),
                customer.getSegmentScore(),
                customer.getPoliticalExposureStatus().name(),
                customer.getEmploymentCategory().name(),
                toAddress(customer.getAddress()),
                customer.isDeleted(),
                customer.getVersion(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private AddressDto toAddress(CustomerAddress address) {
        if (address == null) return null;
        return new AddressDto(name(address.getType()), address.getRawAddress(), address.getCountry(), address.getCountryCode(),
                address.getProvince(), address.getDistrict(), address.getNeighborhood(), address.getRoad(),
                address.getBuilding(), address.getBuildingNumber(), address.getEntrance(), address.getFloor(),
                address.getUnit(), address.getPostalCode(), name(address.getParseStatus()), address.getParser(),
                address.getParserVersion());
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
