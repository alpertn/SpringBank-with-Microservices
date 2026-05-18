package com.banking_microservices.admin_service_command.dto;

public record AdminHistoryCommandResponse(
        String requestId,
        String status,
        String persistedAt
) {
}
