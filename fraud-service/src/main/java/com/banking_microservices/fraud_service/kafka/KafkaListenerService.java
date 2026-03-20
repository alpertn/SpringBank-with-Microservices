package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.dto.enums.TransactionStatus;
import com.banking_microservices.fraud_service.repository.KafkaEventRepository;
import com.banking_microservices.fraud_service.model.KafkaEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {

    private static final String EFT_CHECK = "FRAUD_EFT_CHECK";
    private static final String DEPOSIT_CHECK = "FRAUD_DEPOSIT_CHECK";
    private static final String WITHDRAW_CHECK = "FRAUD_WITHDRAW_CHECK";

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime
                            .parse(json.getAsString()))
            .create();

    private final KafkaEventRepository eventRepository;
    private final KafkaSenderService sender;
    private final Supplier<String> currentTime;

    public KafkaListenerService(KafkaEventRepository eventRepository, KafkaSenderService sender, Supplier<String> currentTime) {
        this.eventRepository = eventRepository;
        this.sender = sender;
        this.currentTime = currentTime;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi.", currentTime.get());
        KafkaTransactionTopicMessageDto dto = gson.fromJson(kafkaData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Gecersiz mesaj alindi, atlaniyor.",
                    currentTime.get());
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), EFT_CHECK)) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(),
                    dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(EFT_CHECK)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. UUID: {}",
                currentTime.get(), dto.getEventUUID());
        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendTransaction(dto.getEventUUID(), dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void depositListener(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | depositListener -> Metoda veri geldi.", currentTime.get());
        KafkaTransactionTopicMessageDto dto = gson.fromJson(kafkaData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | depositListener -> Gecersiz mesaj alindi, atlaniyor.", currentTime.get());
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), DEPOSIT_CHECK)) {
            log.warn(" ({}) > KafkaListenerService | depositListener -> Zaten islendi, atlaniyor: {}", currentTime.get(),
                    dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(DEPOSIT_CHECK)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | depositListener -> Data islenmek uzere alindi. UUID: {}", currentTime.get(),
                dto.getEventUUID());
        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendDeposit(dto.getEventUUID(), dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void withdrawListener(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | withdrawListener -> Metoda veri geldi.", currentTime.get());
        KafkaTransactionTopicMessageDto dto = gson.fromJson(kafkaData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | withdrawListener -> Gecersiz mesaj alindi, atlaniyor.", currentTime.get());
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), WITHDRAW_CHECK)) {
            log.warn(" ({}) > KafkaListenerService | withdrawListener -> Zaten islendi, atlaniyor: {}", currentTime.get(),
                    dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(WITHDRAW_CHECK)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | withdrawListener -> Data islenmek uzere alindi. UUID: {}", currentTime.get(),
                dto.getEventUUID());
        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendWithdraw(dto.getEventUUID(), dto);
    }
}