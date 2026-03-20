package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.model.KafkaEvent;
import com.banking_microservices.transaction_service.repository.KafkaEventRepository;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import com.banking_microservices.transaction_service.service.TransactionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class KafkaListenerService {

    private static final String EFT_RECEIVE      = "TX_EFT_RECEIVE";
    private static final String ERROR_RECEIVE    = "TX_ERROR_RECEIVE";
    private static final String DEPOSIT_SUCCESS  = "TX_DEPOSIT_SUCCESS";
    private static final String WITHDRAW_SUCCESS = "TX_WITHDRAW_SUCCESS";

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final TransactionService transactionService;
    private final KafkaEventRepository eventRepository;
    private final TransactionRepository transactionRepository;
    private final java.util.function.Supplier<String> currentTime;

    public KafkaListenerService(TransactionService transactionService,
                                KafkaEventRepository eventRepository,
                                TransactionRepository transactionRepository,
                                java.util.function.Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.eventRepository = eventRepository;
        this.transactionRepository = transactionRepository;
        this.currentTime = currentTime;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), EFT_RECEIVE)) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(EFT_RECEIVE)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenErrorTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), ERROR_RECEIVE)) {
            log.warn(" ({}) > KafkaListenerService | listenErrorTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(ERROR_RECEIVE)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topicPattern = "${kafka.topics.transaction.logger.listener}")
    public void listenAllTransactionTopics(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenAllTransactionTopics -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> event: {} status: {}, Dto: {}", currentTime.get(), dto.getEventUUID(), dto.getStatus(), gson.toJson(dto));
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositSuccessTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), DEPOSIT_SUCCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(DEPOSIT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Deposit status guncellendi: {} → {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawSuccessTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), WITHDRAW_SUCCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(WITHDRAW_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Withdraw status guncellendi: {} → {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
        }
    }
}