
package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.service.TransactionService;
import com.banking_microservices.transaction_service.repository.KafkaEventRepository;
import com.banking_microservices.transaction_service.model.KafkaEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {
    private final Gson gson = new GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(LocalDateTime.class,
            (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                LocalDateTime.parse(json.getAsString()))
        .create();
    private final TransactionService transactionService;
    private final KafkaEventRepository eventRepository;

    public KafkaListenerService(TransactionService transactionService, KafkaEventRepository eventRepository) {
        this.transactionService = transactionService;
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topicPattern = "${kafka.topics.transaction.logger.listener}")
    public void listenAllTransactionTopics(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);

        String eventId = dto.getEventUUID();
        String status = "PROCESSED";
        if (Boolean.TRUE.equals(dto.getIsMoneyBlocked())) {
            status = "Money Blocked";
        } else if (Boolean.TRUE.equals(dto.getError())) {
            status = "Error: " + dto.getErrorDescription();
        } else {
            status = dto.getStatus();
        }

        KafkaEvent event = eventRepository.findById(eventId).orElse(
                KafkaEvent.builder()
                        .eventId(eventId)
                        .createdAt(LocalDateTime.now())
                        .build());
        event.setStatus(status);
        event.setTopicName("transaction-logger");
        eventRepository.save(event);
    }
}
