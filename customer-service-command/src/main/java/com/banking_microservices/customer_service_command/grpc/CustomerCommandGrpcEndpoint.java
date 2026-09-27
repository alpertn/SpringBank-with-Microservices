package com.banking_microservices.customer_service_command.grpc;

import com.banking_microservices.customer_service_command.dto.CreateCustomerCommandRequest;
import com.banking_microservices.customer_service_command.dto.AddressInput;
import com.banking_microservices.customer_service_command.dto.AddressDto;
import com.banking_microservices.customer_service_command.dto.CustomerResponseDto;
import com.banking_microservices.customer_service_command.dto.UpdateContactVerificationRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerProfileRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateKycStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateMfaPreferenceRequest;
import com.banking_microservices.customer_service_command.dto.UpdateRiskScoreRequest;
import com.banking_microservices.customer_service_command.dto.UpdateSpecialCustomerRequest;
import com.banking_microservices.customer_service_command.dto.enums.KeycloakRealm;
import com.banking_microservices.customer_service_command.dto.enums.AccountStatus;
import com.banking_microservices.customer_service_command.dto.enums.AddressType;
import com.banking_microservices.customer_service_command.dto.enums.CustomerSegment;
import com.banking_microservices.customer_service_command.dto.enums.EmploymentCategory;
import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.dto.enums.NationalityCode;
import com.banking_microservices.customer_service_command.dto.enums.MaritalStatus;
import com.banking_microservices.customer_service_command.dto.enums.PoliticalExposureStatus;
import com.banking_microservices.customer_service_command.dto.enums.RiskClass;
import com.banking_microservices.customer_service_command.dto.enums.Sex;
import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.dto.enums.UserType;
import com.banking_microservices.customer_service_command.exception.CustomerAlreadyExistsException;
import com.banking_microservices.customer_service_command.exception.AddressParsingException;
import com.banking_microservices.customer_service_command.exception.CustomerNotFoundException;
import com.banking_microservices.customer_service_command.service.CustomerCommandService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@GrpcService
@RequiredArgsConstructor
public class CustomerCommandGrpcEndpoint extends CustomerCommandGrpcServiceGrpc.CustomerCommandGrpcServiceImplBase {

    private final CustomerCommandService customerCommandService;

    @Override
    public void createCustomer(CustomerCommandCreateRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.createCustomer(CreateCustomerCommandRequest.builder()
                .keycloakId(uuid(request.getKeycloakId()))
                .realm(enumValue(KeycloakRealm.class, request.getRealm(), KeycloakRealm.BANKING))
                .email(request.getEmail())
                .emailVerified(request.getEmailVerified())
                .phoneNumber(request.getPhoneNumber())
                .phoneVerified(request.getPhoneVerified())
                .userType(enumValue(UserType.class, request.getUserType(), UserType.INDIVIDUAL))
                .status(enumValue(UserStatus.class, request.getStatus(), UserStatus.ACTIVE))
                .birthdate(date(request.getBirthdate()))
                .name(request.getName())
                .middleName(request.getMiddleName())
                .surname(request.getSurname())
                .sex(enumValue(Sex.class, request.getSex(), Sex.UNSPECIFIED))
                .riskScore(request.getRiskScore())
                .mfaEnabled(request.getMfaEnabled())
                .mfaMethod(enumValue(MfaMethod.class, request.getMfaMethod(), MfaMethod.NONE))
                .preferredLanguage(request.getPreferredLanguage())
                .specialCustomer(request.getSpecialCustomer())
                .specialCustomerScore(request.getSpecialCustomerScore())
                .kycStatus(enumValue(KycStatus.class, request.getKycStatus(), KycStatus.PENDING))
                .nationalityCode(enumValue(NationalityCode.class, request.getNationalityCode(), NationalityCode.TR))
                .nationalId(request.getNationalId())
                .accountStatus(enumValue(AccountStatus.class, request.getAccountStatus(), AccountStatus.PENDING_ACTIVATION))
                .maritalStatus(enumValue(MaritalStatus.class, request.getMaritalStatus(), MaritalStatus.UNKNOWN))
                .riskClass(enumValue(RiskClass.class, request.getRiskClass(), RiskClass.RISK_1))
                .segment(enumValue(CustomerSegment.class, request.getSegment(), CustomerSegment.STANDARD))
                .segmentScore(request.getSegmentScore())
                .politicalExposureStatus(enumValue(PoliticalExposureStatus.class, request.getPoliticalExposureStatus(), PoliticalExposureStatus.NONE))
                .employmentCategory(enumValue(EmploymentCategory.class, request.getEmploymentCategory(), EmploymentCategory.OTHER))
                .address(new AddressInput(
                        enumValue(AddressType.class, request.getAddress().getType(), AddressType.RESIDENTIAL),
                        request.getAddress().getRawAddress(), request.getAddress().getCountryCode()))
                .build()), responseObserver);
    }

    @Override
    public void updateProfile(CustomerCommandUpdateProfileRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateProfile(UpdateCustomerProfileRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .email(emptyToNull(request.getEmail()))
                .phoneNumber(emptyToNull(request.getPhoneNumber()))
                .userType(enumValueOrNull(UserType.class, request.getUserType()))
                .birthdate(date(request.getBirthdate()))
                .name(emptyToNull(request.getName()))
                .middleName(emptyToNull(request.getMiddleName()))
                .surname(emptyToNull(request.getSurname()))
                .sex(enumValueOrNull(Sex.class, request.getSex()))
                .preferredLanguage(emptyToNull(request.getPreferredLanguage()))
                .nationalityCode(enumValueOrNull(NationalityCode.class, request.getNationalityCode()))
                .build()), responseObserver);
    }

    @Override
    public void updateContactVerification(CustomerCommandContactVerificationRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateContactVerification(UpdateContactVerificationRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .emailVerified(request.getEmailVerified())
                .phoneVerified(request.getPhoneVerified())
                .build()), responseObserver);
    }

    @Override
    public void updateStatus(CustomerCommandStatusRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateStatus(UpdateCustomerStatusRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .status(enumValue(UserStatus.class, request.getStatus(), UserStatus.ACTIVE))
                .build()), responseObserver);
    }

    @Override
    public void updateKycStatus(CustomerCommandKycRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateKycStatus(UpdateKycStatusRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .kycStatus(enumValue(KycStatus.class, request.getKycStatus(), KycStatus.PENDING))
                .build()), responseObserver);
    }

    @Override
    public void updateRiskScore(CustomerCommandRiskRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateRiskScore(UpdateRiskScoreRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .riskScore(request.getRiskScore())
                .build()), responseObserver);
    }

    @Override
    public void updateMfa(CustomerCommandMfaRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateMfa(UpdateMfaPreferenceRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .mfaEnabled(request.getMfaEnabled())
                .mfaMethod(enumValue(MfaMethod.class, request.getMfaMethod(), MfaMethod.NONE))
                .build()), responseObserver);
    }

    @Override
    public void updateSpecialCustomer(CustomerCommandSpecialRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.updateSpecialCustomer(UpdateSpecialCustomerRequest.builder()
                .customerId(uuid(request.getCustomerId()))
                .specialCustomer(request.getSpecialCustomer())
                .specialCustomerScore(request.getSpecialCustomerScore())
                .build()), responseObserver);
    }

    @Override
    public void softDelete(CustomerCommandCustomerIdRequest request, StreamObserver<CustomerCommandResponse> responseObserver) {
        respond(() -> customerCommandService.softDelete(uuid(request.getCustomerId())), responseObserver);
    }

    private void respond(Supplier<CustomerResponseDto> supplier, StreamObserver<CustomerCommandResponse> responseObserver) {
        try {
            responseObserver.onNext(toGrpc(supplier.get()));
            responseObserver.onCompleted();
        } catch (CustomerAlreadyExistsException exception) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (CustomerNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (AddressParsingException exception) {
            responseObserver.onError(Status.UNAVAILABLE.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("customer-service-command gRPC failed").withCause(exception).asRuntimeException());
        }
    }

    private CustomerCommandResponse toGrpc(CustomerResponseDto dto) {
        return CustomerCommandResponse.newBuilder()
                .setId(value(dto.id()))
                .setKeycloakId(value(dto.keycloakId()))
                .setRealm(value(dto.realm()))
                .setEmail(value(dto.email()))
                .setEmailVerified(dto.emailVerified())
                .setPhoneNumber(value(dto.phoneNumber()))
                .setPhoneVerified(dto.phoneVerified())
                .setUserType(value(dto.userType()))
                .setStatus(value(dto.status()))
                .setBirthdate(value(dto.birthdate()))
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
                .setVersion(dto.version() == null ? 0 : dto.version())
                .setCreatedAt(value(dto.createdAt()))
                .setUpdatedAt(value(dto.updatedAt()))
                .setNationalIdMasked(value(dto.nationalIdMasked()))
                .setAccountStatus(value(dto.accountStatus()))
                .setMaritalStatus(value(dto.maritalStatus()))
                .setRiskClass(value(dto.riskClass()))
                .setSegment(value(dto.segment()))
                .setSegmentScore(dto.segmentScore())
                .setPoliticalExposureStatus(value(dto.politicalExposureStatus()))
                .setEmploymentCategory(value(dto.employmentCategory()))
                .setAddress(toGrpc(dto.address()))
                .build();
    }

    private CustomerAddress toGrpc(AddressDto address) {
        if (address == null) return CustomerAddress.getDefaultInstance();
        return CustomerAddress.newBuilder()
                .setType(value(address.type())).setRawAddress(value(address.rawAddress()))
                .setCountry(value(address.country())).setCountryCode(value(address.countryCode()))
                .setProvince(value(address.province())).setDistrict(value(address.district()))
                .setNeighborhood(value(address.neighborhood())).setRoad(value(address.road()))
                .setBuilding(value(address.building())).setBuildingNumber(value(address.buildingNumber()))
                .setEntrance(value(address.entrance())).setFloor(value(address.floor())).setUnit(value(address.unit()))
                .setPostalCode(value(address.postalCode())).setParseStatus(value(address.parseStatus()))
                .setParser(value(address.parser())).setParserVersion(value(address.parserVersion())).build();
    }

    private UUID uuid(String value) {
        return UUID.fromString(value);
    }

    private LocalDateTime date(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        return value == null || value.isBlank() ? fallback : Enum.valueOf(type, value.toUpperCase());
    }

    private <E extends Enum<E>> E enumValueOrNull(Class<E> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value.toUpperCase());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
