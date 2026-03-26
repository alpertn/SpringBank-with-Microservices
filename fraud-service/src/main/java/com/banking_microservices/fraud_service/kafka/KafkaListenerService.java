package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.dto.enums.KafkaEventType;
import com.banking_microservices.fraud_service.dto.enums.TransactionStatus;
import com.banking_microservices.fraud_service.repository.KafkaEventRepository;
import com.banking_microservices.fraud_service.model.KafkaEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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
            .setPrettyPrinting()
            .create();

    private final KafkaEventRepository eventRepository;
    private final KafkaSenderService sender;
    private final Supplier<String> currentTime;

    public KafkaListenerService(KafkaEventRepository eventRepository, KafkaSenderService sender, Supplier<String> currentTime) {
        this.eventRepository = eventRepository;
        this.sender = sender;
        this.currentTime = currentTime;
    }

    // ─── Listeners ──────────────────────────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi.", currentTime.get());

        KafkaTransactionTopicMessageDto dto = parseMessage(kafkaData, "listenTransactionTopic");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.EFT_CHECK_RECEIVED, KafkaEventType.EFT_CHECK_DONE, "listenTransactionTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. UUID: {}", currentTime.get(), dto.getEventUUID());

        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendTransaction(dto.getEventUUID(), dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.EFT_CHECK_DONE);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void depositListener(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | depositListener -> Metoda veri geldi.", currentTime.get());

        KafkaTransactionTopicMessageDto dto = parseMessage(kafkaData, "depositListener");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.DEPOSIT_CHECK_RECEIVED, KafkaEventType.DEPOSIT_CHECK_DONE, "depositListener")) return;

        log.info(" ({}) > KafkaListenerService | depositListener -> Data islenmek uzere alindi. UUID: {}", currentTime.get(), dto.getEventUUID());

        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendDeposit(dto.getEventUUID(), dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.DEPOSIT_CHECK_DONE);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void withdrawListener(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | withdrawListener -> Metoda veri geldi.", currentTime.get());

        KafkaTransactionTopicMessageDto dto = parseMessage(kafkaData, "withdrawListener");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.WITHDRAW_CHECK_RECEIVED, KafkaEventType.WITHDRAW_CHECK_DONE, "withdrawListener")) return;

        log.info(" ({}) > KafkaListenerService | withdrawListener -> Data islenmek uzere alindi. UUID: {}", currentTime.get(), dto.getEventUUID());

        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendWithdraw(dto.getEventUUID(), dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.WITHDRAW_CHECK_DONE);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Kafka'dan gelen raw JSON'u {@link KafkaTransactionTopicMessageDto}'ya ceviri ve bos UUID kontrolu yapar.
     * Gecersiz veri gelirse null doner, cagiran listener hemen return etmelidir.
     */
    private KafkaTransactionTopicMessageDto parseMessage(String kafkaData, String method) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(kafkaData, KafkaTransactionTopicMessageDto.class);
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