package com.banking_microservices.admin_service_command.kafka;

import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandRequest;
import com.banking_microservices.admin_service_command.service.AdminHistoryCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminHistoryCommandListener {

    private final AdminHistoryCommandService adminHistoryCommandService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${admin-service.topics.command}")
    public void consume(String payload) throws Exception {
        AdminHistoryCommandRequest request = objectMapper.readValue(payload, AdminHistoryCommandRequest.class);
        log.info("Admin history command consumed. requestId={}, status={}, transport={}",
                request.requestId(), request.status(), request.transport());
        adminHistoryCommandService.upsert(request);
    }
}
