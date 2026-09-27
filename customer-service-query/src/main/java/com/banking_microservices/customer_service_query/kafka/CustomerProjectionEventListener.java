package com.banking_microservices.customer_service_query.kafka;

import com.banking_microservices.customer_service_query.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_query.service.CustomerProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerProjectionEventListener {

    private final CustomerProjectionService customerProjectionService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @KafkaListener(topics = "${customer-service.topics.projection-sync}")
    public void consume(String payload) throws Exception {
        CustomerProjectionEvent event = objectMapper.readValue(payload, CustomerProjectionEvent.class);
        log.info("Customer projection event consumed. eventId={}, aggregateId={}, operation={}",
                event.getEventId(), event.getAggregateId(), event.getOperationType());
        customerProjectionService.project(event);
    }
}
