package com.banking_microservices.money_service_query.grpc;

import com.banking_microservices.money_service_query.dto.MoneyAccountReadDto;
import com.banking_microservices.money_service_query.exception.ReadModelNotFoundException;
import io.grpc.Status;
import com.banking_microservices.money_service_query.service.MoneyQueryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Objects;

@GrpcService
@RequiredArgsConstructor
public class MoneyQueryGrpcEndpoint extends MoneyQueryGrpcServiceGrpc.MoneyQueryGrpcServiceImplBase {

    // gRPC endpoint servisler arasi hizli query cevaplari doner.
    private final MoneyQueryService moneyQueryService;

    @Override
    public void getAccountById(MoneyAccountByIdRequest request, StreamObserver<MoneyAccountResponse> responseObserver) {
        respond(() -> moneyQueryService.getById(request.getId()), responseObserver);
    }

    @Override
    public void getAccountByUserId(MoneyAccountByUserIdRequest request,
                                   StreamObserver<MoneyAccountResponse> responseObserver) {
        respond(() -> moneyQueryService.getByUserId(request.getUserId()), responseObserver);
    }

    @Override
    public void getAccountByIban(MoneyAccountByIbanRequest request, StreamObserver<MoneyAccountResponse> responseObserver) {
        respond(() -> moneyQueryService.getByIban(request.getIban()), responseObserver);
    }

    private void respond(java.util.function.Supplier<MoneyAccountReadDto> supplier,
                         StreamObserver<MoneyAccountResponse> responseObserver) {
        try {
            MoneyAccountReadDto dto = supplier.get();
            responseObserver.onNext(MoneyAccountResponse.newBuilder()
                    .setId(Objects.toString(dto.id(), ""))
                    .setUserId(Objects.toString(dto.userId(), ""))
                    .setKeycloakUserUuid(Objects.toString(dto.keycloakUserUUID(), ""))
                    .setUserIban(Objects.toString(dto.userIban(), ""))
                    .setAvailableBalance(Objects.toString(dto.availableBalance(), "0.00"))
                    .setBlockedBalance(Objects.toString(dto.blockedBalance(), "0.00"))
                    .build());
            responseObserver.onCompleted();
        } catch (ReadModelNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("money-service-query gRPC failed")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
