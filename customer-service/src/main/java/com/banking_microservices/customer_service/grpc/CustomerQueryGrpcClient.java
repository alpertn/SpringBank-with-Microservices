package com.banking_microservices.customer_service.grpc;

import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.dto.AddressReadDto;
import com.banking_microservices.customer_service.exception.CustomerQueryClientException;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerQueryGrpcClient {

    @GrpcClient("customer-query")
    private CustomerQueryGrpcServiceGrpc.CustomerQueryGrpcServiceBlockingStub stub;

    public CustomerReadDto getById(String id) {
        try {
            return toDto(stub.getById(CustomerByIdRequest.newBuilder().setId(value(id)).build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query getById failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto getByKeycloakId(String keycloakId) {
        try {
            return toDto(stub.getByKeycloakId(CustomerByKeycloakIdRequest.newBuilder().setKeycloakId(value(keycloakId)).build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query getByKeycloakId failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto getByEmail(String email) {
        try {
            return toDto(stub.getByEmail(CustomerByEmailRequest.newBuilder().setEmail(value(email)).build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query getByEmail failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public CustomerReadDto getByPhoneNumber(String phoneNumber) {
        try {
            return toDto(stub.getByPhoneNumber(CustomerByPhoneRequest.newBuilder().setPhoneNumber(value(phoneNumber)).build()));
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query getByPhoneNumber failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public List<CustomerReadDto> search(String keyword, int limit) {
        try {
            return stub.search(CustomerSearchRequest.newBuilder().setKeyword(value(keyword)).setLimit(limit).build())
                    .getItemsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query search failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public List<CustomerReadDto> listByStatus(String status, int limit) {
        try {
            return stub.listByStatus(CustomerListByStatusRequest.newBuilder().setStatus(value(status)).setLimit(limit).build())
                    .getItemsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query listByStatus failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public List<CustomerReadDto> listByKycStatus(String kycStatus, int limit) {
        try {
            return stub.listByKycStatus(CustomerListByKycStatusRequest.newBuilder().setKycStatus(value(kycStatus)).setLimit(limit).build())
                    .getItemsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query listByKycStatus failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public List<CustomerReadDto> listSpecialCustomers(int limit) {
        try {
            return stub.listSpecialCustomers(CustomerSearchRequest.newBuilder().setLimit(limit).build())
                    .getItemsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query listSpecialCustomers failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    public List<CustomerReadDto> listHighRiskCustomers(int minRiskScore, int limit) {
        try {
            return stub.listHighRiskCustomers(CustomerRiskListRequest.newBuilder().setMinRiskScore(minRiskScore).setLimit(limit).build())
                    .getItemsList().stream().map(this::toDto).toList();
        } catch (StatusRuntimeException exception) {
            throw new CustomerQueryClientException("customer-service-query listHighRiskCustomers failed: " + exception.getStatus().getDescription(), exception);
        }
    }

    private CustomerReadDto toDto(CustomerResponse response) {
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
                response.getNationalIdMasked(), response.getAccountStatus(), response.getMaritalStatus(),
                response.getRiskClass(), response.getSegment(), response.getSegmentScore(),
                response.getPoliticalExposureStatus(), response.getEmploymentCategory(), toAddress(response.getAddress()),
                response.getDeleted(),
                response.getLastOperationType(),
                response.getLastSyncedAt()
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private AddressReadDto toAddress(CustomerQueryAddress address) {
        return new AddressReadDto(address.getType(), address.getRawAddress(), address.getCountry(),
                address.getCountryCode(), address.getProvince(), address.getDistrict(), address.getNeighborhood(),
                address.getRoad(), address.getBuilding(), address.getBuildingNumber(), address.getEntrance(),
                address.getFloor(), address.getUnit(), address.getPostalCode(), address.getParseStatus(),
                address.getParser(), address.getParserVersion());
    }
}
