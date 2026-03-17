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
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final TransactionService transactionService;
    private final KafkaEventRepository eventRepository;
    private final TransactionRepository transactionRepository;

    public KafkaListenerService(TransactionService transactionService,
                                KafkaEventRepository eventRepository,
                                TransactionRepository transactionRepository) {
        this.transactionService = transactionService;
        this.eventRepository = eventRepository;
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        log.info("listenTransactionTopic mesaj alindi");
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenTransactionTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), EFT_RECEIVE)) {
            log.warn("listenTransactionTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(EFT_RECEIVE)
                .createdAt(LocalDateTime.now())
                .build());
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData) {
        log.info("listenErrorTopic mesaj alindi");
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenErrorTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), ERROR_RECEIVE)) {
            log.warn("listenErrorTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(ERROR_RECEIVE)
                .createdAt(LocalDateTime.now())
                .build());
        transactionService.saveTransaction(dto);
    }

    @KafkaListener(topicPattern = "${kafka.topics.transaction.logger.listener}")
    public void listenAllTransactionTopics(String topicData) {
        log.info("listenAllTransactionTopics mesaj alindi");
        // Log-only listener — idempotency gerekmez, sadece loglama
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenAllTransactionTopics - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        log.info("listenAllTransactionTopics - event: {} status: {}", dto.getEventUUID(), dto.getStatus());
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositSuccessTopic(String topicData) {
        log.info("listenDepositSuccessTopic mesaj alindi");
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenDepositSuccessTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), DEPOSIT_SUCCESS)) {
            log.warn("listenDepositSuccessTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(DEPOSIT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
        try {
            transactionService.updateTransactionStatus(dto);
            log.info("Deposit status guncellendi: {} → {}", dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error("listenDepositSuccessTopic - updateTransactionStatus hatasi: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawSuccessTopic(String topicData) {
        log.info("listenWithdrawSuccessTopic mesaj alindi");
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenWithdrawSuccessTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), WITHDRAW_SUCCESS)) {
            log.warn("listenWithdrawSuccessTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(WITHDRAW_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
        try {
            transactionService.updateTransactionStatus(dto);
            log.info("Withdraw status guncellendi: {} → {}", dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error("listenWithdrawSuccessTopic - updateTransactionStatus hatasi: {}", e.getMessage(), e);
        }
    }
}