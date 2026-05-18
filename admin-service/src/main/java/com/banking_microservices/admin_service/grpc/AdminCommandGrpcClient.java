package com.banking_microservices.admin_service.grpc;

import com.banking_microservices.admin_service.dto.AdminHistoryCommandRequest;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCommandGrpcClient {

    @GrpcClient("admin-command")
    private AdminCommandGrpcServiceGrpc.AdminCommandGrpcServiceBlockingStub stub;

    public void upsert(AdminHistoryCommandRequest request) {
        stub.upsertHistory(AdminHistoryWriteRequest.newBuilder()
                .setRequestId(value(request.requestId()))
                .setAdminEmail(value(request.adminEmail()))
                .setAdminPasswordMasked(value(request.adminPasswordMasked()))
                .setTransport(value(request.transport()))
                .setRequestType(value(request.requestType()))
                .setTargetType(value(request.targetType()))
                .setTargetName(value(request.targetName()))
                .setTopicName(value(request.topicName()))
                .setStatus(value(request.status()))
                .setResponseReceived(request.responseReceived())
                .setResponseType(value(request.responseType()))
                .setQueryText(value(request.queryText()))
                .setRequestPayload(value(request.requestPayload()))
                .setResponsePayload(value(request.responsePayload()))
                .setErrorMessage(value(request.errorMessage()))
                .setRequestedAt(value(request.requestedAt()))
                .setReceivedAt(value(request.receivedAt()))
                .build());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
