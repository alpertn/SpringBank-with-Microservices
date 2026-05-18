package com.banking_microservices.admin_service_query.kafka;

import com.banking_microservices.admin_service_query.dto.AdminHistoryProjectionEvent;
import com.banking_microservices.admin_service_query.service.AdminHistoryProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminHistoryProjectionListener {

    private final AdminHistoryProjectionService adminHistoryProjectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${admin-service.topics.projection-sync}")
    public void consume(String payload) throws Exception {
        AdminHistoryProjectionEvent event = objectMapper.readValue(payload, AdminHistoryProjectionEvent.class);
        log.info("Admin history projection event consumed. requestId={}, status={}", event.getRequestId(), event.getStatus());
        adminHistoryProjectionService.project(event);
    }
}
