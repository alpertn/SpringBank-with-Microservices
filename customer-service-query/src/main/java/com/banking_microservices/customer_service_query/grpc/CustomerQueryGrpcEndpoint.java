package com.banking_microservices.customer_service_query.grpc;

import com.banking_microservices.customer_service_query.dto.CustomerReadDto;
import com.banking_microservices.customer_service_query.dto.AddressDto;
import com.banking_microservices.customer_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.customer_service_query.service.CustomerQueryService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CustomerQueryGrpcEndpoint extends CustomerQueryGrpcServiceGrpc.CustomerQueryGrpcServiceImplBase {

    private final CustomerQueryService customerQueryService;

    @Override
    public void getById(CustomerByIdRequest request, StreamObserver<CustomerResponse> responseObserver) {
        respondOne(() -> customerQueryService.getById(request.getId()), responseObserver);
    }

    @Override
    public void getByKeycloakId(CustomerByKeycloakIdRequest request, StreamObserver<CustomerResponse> responseObserver) {
        respondOne(() -> customerQueryService.getByKeycloakId(request.getKeycloakId()), responseObserver);
    }

    @Override
    public void getByEmail(CustomerByEmailRequest request, StreamObserver<CustomerResponse> responseObserver) {
        respondOne(() -> customerQueryService.getByEmail(request.getEmail()), responseObserver);
    }

    @Override
    public void getByPhoneNumber(CustomerByPhoneRequest request, StreamObserver<CustomerResponse> responseObserver) {
        respondOne(() -> customerQueryService.getByPhoneNumber(request.getPhoneNumber()), responseObserver);
    }

    @Override
    public void search(CustomerSearchRequest request, StreamObserver<CustomerListResponse> responseObserver) {
        respondList(() -> customerQueryService.search(request.getKeyword(), request.getLimit()), responseObserver);
    }

    @Override
    public void listByStatus(CustomerListByStatusRequest request, StreamObserver<CustomerListResponse> responseObserver) {
        respondList(() -> customerQueryService.listByStatus(request.getStatus(), request.getLimit()), responseObserver);
    }

    @Override
    public void listByKycStatus(CustomerListByKycStatusRequest request, StreamObserver<CustomerListResponse> responseObserver) {
        respondList(() -> customerQueryService.listByKycStatus(request.getKycStatus(), request.getLimit()), responseObserver);
    }

    @Override
    public void listSpecialCustomers(CustomerSearchRequest request, StreamObserver<CustomerListResponse> responseObserver) {
        respondList(() -> customerQueryService.listSpecialCustomers(request.getLimit()), responseObserver);
    }

    @Override
    public void listHighRiskCustomers(CustomerRiskListRequest request, StreamObserver<CustomerListResponse> responseObserver) {
        respondList(() -> customerQueryService.listHighRiskCustomers(request.getMinRiskScore(), request.getLimit()), responseObserver);
    }

    private void respondOne(java.util.function.Supplier<CustomerReadDto> supplier,
                            StreamObserver<CustomerResponse> responseObserver) {
        try {
            responseObserver.onNext(toGrpc(supplier.get()));
            responseObserver.onCompleted();
        } catch (ReadModelNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("customer-service-query gRPC failed").withCause(exception).asRuntimeException());
        }
    }

    private void respondList(java.util.function.Supplier<java.util.List<CustomerReadDto>> supplier,
                             StreamObserver<CustomerListResponse> responseObserver) {
        try {
            CustomerListResponse.Builder builder = CustomerListResponse.newBuilder();
            supplier.get().forEach(item -> builder.addItems(toGrpc(item)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("customer-service-query gRPC list failed").withCause(exception).asRuntimeException());
        }
    }

    private CustomerResponse toGrpc(CustomerReadDto dto) {
        return CustomerResponse.newBuilder()
                .setId(value(dto.id()))
                .setKeycloakId(value(dto.keycloakId()))
                .setRealm(value(dto.realm()))
                .setEmail(value(dto.email()))
                .setEmailVerified(dto.emailVerified())
                .setPhoneNumber(value(dto.phoneNumber()))
                .setPhoneVerified(dto.phoneVerified())
                .setUserType(value(dto.userType()))
                .setStatus(value(dto.status()))
                .setBirthdate(dto.birthdate() == null ? "" : dto.birthdate().toString())
                .setName(value(dto.name()))
                .setMiddleName(value(dto.middleName()))
                .setSurname(value(dto.surname()))
                .setSex(value(dto.sex()))
                .setRiskScore(dto.riskScore())
                .setMfaEnabled(dto.mfaEnabled())
                .setMfaMethod(value(dto.mfaMethod()))
                .setPreferredLanguage(value(dto.preferredLanguage()))
                .setSpecialCustomer(dto.specialCustomer())
                .setSpecialCustomerScore(dto.specialCustomerScore())
                .setKycStatus(value(dto.kycStatus()))
                .setNationalityCode(value(dto.nationalityCode()))
                .setDeleted(dto.deleted())
                .setLastOperationType(value(dto.lastOperationType()))
                .setLastSyncedAt(dto.lastSyncedAt() == null ? "" : dto.lastSyncedAt().toString())
                .setNationalIdMasked(value(dto.nationalIdMasked()))
                .setAccountStatus(value(dto.accountStatus())).setMaritalStatus(value(dto.maritalStatus()))
                .setRiskClass(value(dto.riskClass())).setSegment(value(dto.segment())).setSegmentScore(dto.segmentScore())
                .setPoliticalExposureStatus(value(dto.politicalExposureStatus()))
                .setEmploymentCategory(value(dto.employmentCategory())).setAddress(toGrpc(dto.address()))
                .build();
    }

    private CustomerAddress toGrpc(AddressDto address) {
        if (address == null) return CustomerAddress.getDefaultInstance();
        return CustomerAddress.newBuilder().setType(value(address.getType())).setRawAddress(value(address.getRawAddress()))
                .setCountry(value(address.getCountry())).setCountryCode(value(address.getCountryCode()))
                .setProvince(value(address.getProvince())).setDistrict(value(address.getDistrict()))
                .setNeighborhood(value(address.getNeighborhood())).setRoad(value(address.getRoad()))
                .setBuilding(value(address.getBuilding())).setBuildingNumber(value(address.getBuildingNumber()))
                .setEntrance(value(address.getEntrance())).setFloor(value(address.getFloor())).setUnit(value(address.getUnit()))
                .setPostalCode(value(address.getPostalCode())).setParseStatus(value(address.getParseStatus()))
                .setParser(value(address.getParser())).setParserVersion(value(address.getParserVersion())).build();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
