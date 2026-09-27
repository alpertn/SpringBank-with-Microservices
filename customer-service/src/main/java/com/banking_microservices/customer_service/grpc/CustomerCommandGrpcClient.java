package com.banking_microservices.customer_service.grpc;

import com.banking_microservices.customer_service.dto.CreateCustomerRequest;
import com.banking_microservices.customer_service.dto.AddressReadDto;
import com.banking_microservices.customer_service.dto.CustomerContactVerificationRequest;
import com.banking_microservices.customer_service.dto.CustomerKycUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerMfaUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerProfileUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.dto.CustomerRiskUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerSpecialUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerStatusUpdateRequest;
import com.banking_microservices.customer_service.exception.CustomerCommandClientException;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class CustomerCommandGrpcClient {

    @GrpcClient("customer-command")
    private CustomerCommandGrpcServiceGrpc.CustomerCommandGrpcServiceBlockingStub stub;

    public CustomerReadDto createCustomer(CreateCustomerRequest request) {
        try {
            return toDto(stub.createCustomer(CustomerCommandCreateRequest.newBuilder()
                    .setKeycloakId(value(request.keycloakId()))
                    .setRealm("BANKING")
                    .setEmail(value(request.email()))
                    .setEmailVerified(request.emailVerified())
                    .setPhoneNumber(value(request.phoneNumber()))
                    .setPhoneVerified(request.phoneVerified())
                    .setUserType("INDIVIDUAL")
                    .setStatus("ACTIVE")
                    .setBirthdate(value(request.birthdate()))
                    .setName(value(request.name()))
                    .setMiddleName(value(request.middleName()))
                    .setSurname(value(request.surname()))
                    .setSex("UNSPECIFIED")
                    .setRiskScore(0)
                    .setMfaEnabled(false)
                    .setMfaMethod("NONE")
                    .setPreferredLanguage("tr")
                    .setSpecialCustomer(false)
                    .setSpecialCustomerScore(0)
                    .setKycStatus("PENDING")
                    .setNationalId(value(request.nationalId()))
                    .setNationalityCode(defaultValue(request.nationalityCode(), "TR"))
                    .setMaritalStatus(defaultValue(request.maritalStatus(), "UNKNOWN"))
                    .setAccountStatus(defaultValue(request.accountStatus(), "PENDING_ACTIVATION"))
                    .setRiskClass(defaultValue(request.riskClass(), "RISK_1"))
                    .setSegment(defaultValue(request.segment(), "STANDARD"))
                    .setSegmentScore(request.segmentScore() == 0 ? 1 : request.segmentScore())
                    .setPoliticalExposureStatus(defaultValue(request.politicalExposureStatus(), "NONE"))
                    .setEmploymentCategory(defaultValue(request.employmentCategory(), "OTHER"))
                    .setAddress(CustomerCommandAddress.newBuilder()
                            .setType(defaultValue(request.address().type(), "RESIDENTIAL"))
                            .setRawAddress(value(request.address().rawAddress()))
                            .setCountryCode(defaultValue(request.address().countryCode(), "TR"))
                            .build())
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command create failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateProfile(CustomerProfileUpdateRequest request) {
        try {
            return toDto(stub.updateProfile(CustomerCommandUpdateProfileRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setEmail(value(request.email()))
                    .setPhoneNumber(value(request.phoneNumber()))
                    .setBirthdate(value(request.birthdate()))
                    .setName(value(request.name()))
                    .setMiddleName(value(request.middleName()))
                    .setSurname(value(request.surname()))
                    .setSex(value(request.sex()))
                    .setPreferredLanguage(value(request.preferredLanguage()))
                    .setNationalityCode(value(request.nationalityCode()))
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command profile update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateContactVerification(CustomerContactVerificationRequest request) {
        try {
            return toDto(stub.updateContactVerification(CustomerCommandContactVerificationRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setEmailVerified(request.emailVerified())
                    .setPhoneVerified(request.phoneVerified())
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command contact verification update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateStatus(CustomerStatusUpdateRequest request) {
        try {
            return toDto(stub.updateStatus(CustomerCommandStatusRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setStatus(value(request.status()))
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command status update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateKycStatus(CustomerKycUpdateRequest request) {
        try {
            return toDto(stub.updateKycStatus(CustomerCommandKycRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setKycStatus(value(request.kycStatus()))
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command KYC update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateRiskScore(CustomerRiskUpdateRequest request) {
        try {
            return toDto(stub.updateRiskScore(CustomerCommandRiskRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setRiskScore(request.riskScore())
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command risk update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateMfaPreference(CustomerMfaUpdateRequest request) {
        try {
            return toDto(stub.updateMfa(CustomerCommandMfaRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setMfaEnabled(request.mfaEnabled())
                    .setMfaMethod(value(request.mfaMethod()))
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command MFA update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto updateSpecialCustomer(CustomerSpecialUpdateRequest request) {
        try {
            return toDto(stub.updateSpecialCustomer(CustomerCommandSpecialRequest.newBuilder()
                    .setCustomerId(value(request.id()))
                    .setSpecialCustomer(request.specialCustomer())
                    .setSpecialCustomerScore(request.specialCustomerScore())
                    .build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command special customer update failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto softDeleteCustomer(String id) {
        try {
            return toDto(stub.softDelete(CustomerCommandCustomerIdRequest.newBuilder().setCustomerId(value(id)).build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerCommandClientException("customer-service-command soft delete failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    private CustomerReadDto toDto(CustomerCommandResponse response) {
        return new CustomerReadDto(
                response.getId(),
                response.getKeycloakId(),
                response.getRealm(),
                response.getEmail(),
                response.getEmailVerified(),
                response.getPhoneNumber(),
                response.getPhoneVerified(),
                response.getUserType(),
                response.getStatus(),
                response.getBirthdate(),
                response.getName(),
                response.getMiddleName(),
                response.getSurname(),
                response.getSex(),
                response.getRiskScore(),
                response.getMfaEnabled(),
                response.getMfaMethod(),
                response.getPreferredLanguage(),
                response.getSpecialCustomer(),
                response.getSpecialCustomerScore(),
                response.getKycStatus(),
                response.getNationalityCode(),
                response.getNationalIdMasked(),
                response.getAccountStatus(),
                response.getMaritalStatus(),
                response.getRiskClass(),
                response.getSegment(),
                response.getSegmentScore(),
                response.getPoliticalExposureStatus(),
                response.getEmploymentCategory(),
                toAddress(response.getAddress()),
                response.getDeleted(),
                "",
                response.getUpdatedAt()
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private AddressReadDto toAddress(CustomerCommandAddress address) {
        return new AddressReadDto(address.getType(), address.getRawAddress(), address.getCountry(),
                address.getCountryCode(), address.getProvince(), address.getDistrict(), address.getNeighborhood(),
                address.getRoad(), address.getBuilding(), address.getBuildingNumber(), address.getEntrance(),
                address.getFloor(), address.getUnit(), address.getPostalCode(), address.getParseStatus(),
                address.getParser(), address.getParserVersion());
    }
}
