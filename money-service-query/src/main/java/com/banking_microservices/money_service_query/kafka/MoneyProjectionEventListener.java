package com.banking_microservices.money_service_query.kafka;

import com.banking_microservices.money_service_query.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_query.service.MoneyProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyProjectionEventListener {

    // Bu listener CQRS read-side'in giris kapisidir.
    // Command tarafindan basilan her projection event burada karsilanir.
    private final MoneyProjectionService moneyProjectionService;

    @KafkaListener(topics = "${money-service.topics.projection-sync}")
    public void consume(MoneyProjectionEvent event) {
        log.info("Projection event consumed from Kafka. eventId={}, aggregateId={}, operation={}",
                event.getEventId(), event.getAggregateId(), event.getOperationType());
        moneyProjectionService.project(event);
    }
}
