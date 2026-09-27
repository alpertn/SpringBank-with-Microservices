package com.banking_microservices.customer_service.grpc;

import com.banking_microservices.customer_service.dto.CreateCustomerRequest;
import com.banking_microservices.customer_service.dto.AddressRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.exception.CustomerCommandClientException;
import com.banking_microservices.customer_service.exception.CustomerOnboardingException;
import com.banking_microservices.customer_service.service.CustomerService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CustomerOnboardingGrpcEndpoint extends CustomerOnboardingGrpcServiceGrpc.CustomerOnboardingGrpcServiceImplBase {

    private final CustomerService customerService;

    @Override
    public void createCustomerForUser(CustomerOnboardingCreateRequest request,
                                      StreamObserver<CustomerOnboardingCreateResponse> responseObserver) {
        try {
            CustomerReadDto response = customerService.createCustomer(new CreateCustomerRequest(
                    request.getKeycloakId(),
                    request.getEmail(),
                    request.getEmailVerified(),
                    request.getPhoneNumber(),
                    request.getPhoneVerified(),
                    request.getBirthdate(),
                    request.getName(),
                    request.getMiddleName(),
                    request.getSurname(),
                    request.getNationalId(),
                    request.getNationalityCode(),
                    request.getMaritalStatus(),
                    "PENDING_ACTIVATION",
                    "RISK_1",
                    "STANDARD",
                    1,
                    "NONE",
                    "OTHER",
                    new AddressRequest("RESIDENTIAL", request.getRawAddress(), request.getAddressCountryCode())
            ));
            responseObserver.onNext(CustomerOnboardingCreateResponse.newBuilder()
                    .setCustomerId(response.id())
                    .setKeycloakId(response.keycloakId())
                    .setStatus(response.status())
                    .build());
            responseObserver.onCompleted();
        } catch (CustomerCommandClientException exception) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        } catch (Exception exception) {
            CustomerOnboardingException wrapped = new CustomerOnboardingException("Customer onboarding failed", exception);
            responseObserver.onError(Status.INTERNAL.withDescription(wrapped.getMessage()).withCause(wrapped).asRuntimeException());
        }
    }
}
