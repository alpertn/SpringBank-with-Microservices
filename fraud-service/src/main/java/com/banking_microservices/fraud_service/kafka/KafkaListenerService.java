package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.dto.enums.TransactionStatus;
import com.banking_microservices.fraud_service.service.service;
import com.banking_microservices.fraud_service.repository.KafkaEventRepository;
import com.banking_microservices.fraud_service.model.KafkaEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final service service;
    private final KafkaEventRepository eventRepository;
    private final KafkaSenderService sender;

    public KafkaListenerService(service service, KafkaEventRepository eventRepository, KafkaSenderService sender) {
        this.service = service;
        this.eventRepository = eventRepository;
        this.sender = sender;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void CreateUserListener(String kafkaData) {

        KafkaTransactionTopicMessageDto transactionRequest = gson.fromJson(kafkaData,
                KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(transactionRequest.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        log.info("CreateUserListener data geldi {}", gson.toJson(kafkaData));

        // Forward to Money Service
        transactionRequest.setStatus(TransactionStatus.FRAUD_REVIEW);
        transactionRequest.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendTransaction(transactionRequest.getEventUUID(), transactionRequest);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void depositListener(String kafkaData) {

        KafkaTransactionTopicMessageDto transactionRequest = gson.fromJson(kafkaData,
                KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(transactionRequest.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        log.info("depositListener data geldi {}", gson.toJson(kafkaData));
        sender.sendTransaction(transactionRequest.getEventUUID(), transactionRequest);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void withdrawListener(String kafkaData) {

        KafkaTransactionTopicMessageDto transactionRequest = gson.fromJson(kafkaData,
                KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(transactionRequest.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        log.info("withdrawListener data geldi {}", gson.toJson(kafkaData));
        sender.sendTransaction(transactionRequest.getEventUUID(), transactionRequest);
    }
}