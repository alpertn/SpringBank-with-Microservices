package com.banking_microservices.admin_service_query.grpc;

import com.banking_microservices.admin_service_query.dto.AdminHistoryReadDto;
import com.banking_microservices.admin_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.admin_service_query.service.AdminHistoryQueryService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class AdminHistoryQueryGrpcEndpoint extends AdminHistoryQueryGrpcServiceGrpc.AdminHistoryQueryGrpcServiceImplBase {

    private final AdminHistoryQueryService adminHistoryQueryService;

    @Override
    public void getHistoryByRequestId(AdminHistoryByRequestIdRequest request,
                                      StreamObserver<AdminHistoryResponse> responseObserver) {
        try {
            responseObserver.onNext(toGrpc(adminHistoryQueryService.getByRequestId(request.getRequestId())));
            responseObserver.onCompleted();
        } catch (ReadModelNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(exception.getMessage()).asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("admin-service-query gRPC failed").withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void listHistory(AdminHistoryListRequest request, StreamObserver<AdminHistoryListResponse> responseObserver) {
        try {
            AdminHistoryListResponse.Builder builder = AdminHistoryListResponse.newBuilder();
            adminHistoryQueryService.list(request.getLimit(), request.getKeyword())
                    .forEach(item -> builder.addItems(toGrpc(item)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("admin-service-query gRPC list failed").withCause(exception).asRuntimeException());
        }
    }

    private AdminHistoryResponse toGrpc(AdminHistoryReadDto dto) {
        return AdminHistoryResponse.newBuilder()
                .setRequestId(value(dto.requestId()))
                .setAdminEmail(value(dto.adminEmail()))
                .setAdminPasswordMasked(value(dto.adminPasswordMasked()))
                .setTransport(value(dto.transport()))
                .setRequestType(value(dto.requestType()))
                .setTargetType(value(dto.targetType()))
                .setTargetName(value(dto.targetName()))
                .setTopicName(value(dto.topicName()))
                .setStatus(value(dto.status()))
                .setResponseReceived(dto.responseReceived())
                .setResponseType(value(dto.responseType()))
                .setQueryText(value(dto.queryText()))
                .setRequestPayload(value(dto.requestPayload()))
                .setResponsePayload(value(dto.responsePayload()))
                .setErrorMessage(value(dto.errorMessage()))
                .setRequestedAt(dto.requestedAt() == null ? "" : dto.requestedAt().toString())
                .setReceivedAt(dto.receivedAt() == null ? "" : dto.receivedAt().toString())
                .build();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
