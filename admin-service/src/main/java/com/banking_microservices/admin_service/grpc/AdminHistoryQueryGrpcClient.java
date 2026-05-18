package com.banking_microservices.admin_service.grpc;

import org.springframework.stereotype.Component;
import net.devh.boot.grpc.client.inject.GrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminHistoryQueryGrpcClient {

    @GrpcClient("admin-query")
    private AdminHistoryQueryGrpcServiceGrpc.AdminHistoryQueryGrpcServiceBlockingStub stub;

    public List<Map<String, Object>> list(int limit, String keyword) {
        AdminHistoryListResponse response = stub.listHistory(AdminHistoryListRequest.newBuilder()
                .setLimit(limit)
                .setKeyword(keyword == null ? "" : keyword)
                .build());
        return response.getItemsList().stream().map(this::toMap).toList();
    }

    public Map<String, Object> get(String requestId) {
        try {
            return toMap(stub.getHistoryByRequestId(AdminHistoryByRequestIdRequest.newBuilder().setRequestId(requestId).build()));
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return pendingHistory(requestId);
            }
            throw exception;
        }
    }

    private Map<String, Object> toMap(AdminHistoryResponse item) {
        return Map.ofEntries(
                Map.entry("requestId", item.getRequestId()),
                Map.entry("adminEmail", item.getAdminEmail()),
                Map.entry("adminPasswordMasked", item.getAdminPasswordMasked()),
                Map.entry("transport", item.getTransport()),
                Map.entry("requestType", item.getRequestType()),
                Map.entry("targetType", item.getTargetType()),
                Map.entry("targetName", item.getTargetName()),
                Map.entry("topicName", item.getTopicName()),
                Map.entry("status", item.getStatus()),
                Map.entry("responseReceived", item.getResponseReceived()),
                Map.entry("responseType", item.getResponseType()),
                Map.entry("queryText", item.getQueryText()),
                Map.entry("requestPayload", item.getRequestPayload()),
                Map.entry("responsePayload", item.getResponsePayload()),
                Map.entry("errorMessage", item.getErrorMessage()),
                Map.entry("requestedAt", item.getRequestedAt()),
                Map.entry("receivedAt", item.getReceivedAt())
        );
    }

    private Map<String, Object> pendingHistory(String requestId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("requestId", requestId);
        item.put("adminEmail", "");
        item.put("adminPasswordMasked", "NOT_STORED");
        item.put("transport", "");
        item.put("requestType", "");
        item.put("targetType", "");
        item.put("targetName", "");
        item.put("topicName", "");
        item.put("status", "PENDING");
        item.put("responseReceived", false);
        item.put("responseType", "");
        item.put("queryText", "");
        item.put("requestPayload", "");
        item.put("responsePayload", "");
        item.put("errorMessage", "");
        item.put("requestedAt", "");
        item.put("receivedAt", "");
        return item;
    }
}
