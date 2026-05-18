package com.banking_microservices.admin_service_command.grpc;

import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandRequest;
import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandResponse;
import com.banking_microservices.admin_service_command.service.AdminHistoryCommandService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class AdminCommandGrpcEndpoint extends AdminCommandGrpcServiceGrpc.AdminCommandGrpcServiceImplBase {

    private final AdminHistoryCommandService adminHistoryCommandService;

    @Override
    public void upsertHistory(AdminHistoryWriteRequest request, StreamObserver<AdminHistoryWriteResponse> responseObserver) {
        try {
            AdminHistoryCommandResponse response = adminHistoryCommandService.upsert(AdminHistoryCommandRequest.builder()
                    .requestId(request.getRequestId())
                    .adminEmail(request.getAdminEmail())
                    .adminPasswordMasked(request.getAdminPasswordMasked())
                    .transport(request.getTransport())
                    .requestType(request.getRequestType())
                    .targetType(request.getTargetType())
                    .targetName(request.getTargetName())
                    .topicName(request.getTopicName())
                    .status(request.getStatus())
                    .responseReceived(request.getResponseReceived())
                    .responseType(request.getResponseType())
                    .queryText(request.getQueryText())
                    .requestPayload(request.getRequestPayload())
                    .responsePayload(request.getResponsePayload())
                    .errorMessage(request.getErrorMessage())
                    .requestedAt(request.getRequestedAt())
                    .receivedAt(request.getReceivedAt())
                    .build());

            responseObserver.onNext(AdminHistoryWriteResponse.newBuilder()
                    .setRequestId(response.requestId())
                    .setStatus(response.status())
                    .setPersistedAt(response.persistedAt())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("admin-service-command gRPC failed")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
