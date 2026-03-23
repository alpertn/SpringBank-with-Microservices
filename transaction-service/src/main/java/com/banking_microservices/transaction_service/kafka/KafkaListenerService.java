package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.dto.enums.KafkaEventType;
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
import java.util.function.Supplier;

@Slf4j
@Service
public class KafkaListenerService {

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
    private final Supplier<String> currentTime;

    public KafkaListenerService(TransactionService transactionService,
                                KafkaEventRepository eventRepository,
                                TransactionRepository transactionRepository,
                                Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.eventRepository = eventRepository;
        this.transactionRepository = transactionRepository;
        this.currentTime = currentTime;
    }

    // ─── Listeners ──────────────────────────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenTransactionTopic");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.TX_EFT_RECEIVED, KafkaEventType.TX_EFT_DONE, "listenTransactionTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        transactionService.saveTransaction(dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.TX_EFT_DONE);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenErrorTopic");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.TX_ERROR_RECEIVED, KafkaEventType.TX_ERROR_DONE, "listenErrorTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        transactionService.saveTransaction(dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.TX_ERROR_DONE);
    }

    @KafkaListener(topicPattern = "${kafka.topics.transaction.logger.listener}")
    public void listenAllTransactionTopics(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenAllTransactionTopics");
        if (dto == null) return;

        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> event: {} status: {}, Dto: {}", currentTime.get(), dto.getEventUUID(), dto.getStatus(), gson.toJson(dto));

        try {
            transactionService.updateTransactionStatus(dto);
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenAllTransactionTopics -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositSuccessTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenDepositSuccessTopic");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.TX_DEPOSIT_RECEIVED, KafkaEventType.TX_DEPOSIT_DONE, "listenDepositSuccessTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> Deposit status guncellendi: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenDepositSuccessTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
            return;
        }

        markAsDone(dto.getEventUUID(), KafkaEventType.TX_DEPOSIT_DONE);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawSuccessTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenWithdrawSuccessTopic");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.TX_WITHDRAW_RECEIVED, KafkaEventType.TX_WITHDRAW_DONE, "listenWithdrawSuccessTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> Withdraw status guncellendi: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenWithdrawSuccessTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
            return;
        }

        markAsDone(dto.getEventUUID(), KafkaEventType.TX_WITHDRAW_DONE);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Kafka'dan gelen raw JSON'u {@link KafkaTransactionTopicMessageDto}'ya ceviri ve bos UUID kontrolu yapar.
     * Gecersiz veri gelirse null doner, cagiran listener hemen return etmelidir.
     */
    private KafkaTransactionTopicMessageDto parseMessage(String topicData, String method) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | {} -> Gecersiz mesaj alindi, atlaniyor.", currentTime.get(), method);
            return null;
        }
        return dto;
    }

    /**
     * Iki adimli idempotency kontrolu:
     * 1. eventUUID icin RECEIVED veya DONE kaydi varsa duplicate kabul eder, true doner.
     * 2. Yoksa RECEIVED kaydini hemen olusturur (isleniyor olarak isaretler) ve false doner.
     * Bu sayede ayni anda iki mesaj gelirse birincisi RECEIVED'i kaydeder,
     * ikincisi gorur ve islememek icin return eder.
     */
    private boolean isDuplicateOrClaim(String eventUUID, KafkaEventType received, KafkaEventType done, String method) {
        boolean alreadyReceived = eventRepository.existsByEventIdAndEventType(eventUUID, received.name());
        boolean alreadyDone     = eventRepository.existsByEventIdAndEventType(eventUUID, done.name());

        if (alreadyReceived || alreadyDone) {
            log.warn(" ({}) > KafkaListenerService | {} -> Zaten islendi veya isleniyor, atlaniyor: {}", currentTime.get(), method, eventUUID);
            return true;
        }

        eventRepository.save(KafkaEvent.builder()
                .eventId(eventUUID)
                .eventType(received.name())
                .createdAt(LocalDateTime.now())
                .build());

        return false;
    }

    /**
     * Islem basariyla tamamlandiginda DONE kaydini olusturur.
     * {@link #isDuplicateOrClaim} ile birlikte kullanilir.
     */
    private void markAsDone(String eventUUID, KafkaEventType done) {
        eventRepository.save(KafkaEvent.builder()
                .eventId(eventUUID)
                .eventType(done.name())
                .createdAt(LocalDateTime.now())
                .build());
    }
}