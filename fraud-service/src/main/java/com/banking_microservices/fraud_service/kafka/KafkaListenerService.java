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
    public void listenAllTransactions(String kafkaData) {
        log.info(" ({}) > KafkaListenerService | listenAllTransactions -> Metoda veri geldi.", currentTime.get());

        KafkaTransactionTopicMessageDto dto = parseMessage(kafkaData, "listenAllTransactions");
        if (dto == null) return;

        if (isDuplicateOrClaim(dto.getEventUUID(), KafkaEventType.EFT_CHECK_RECEIVED, KafkaEventType.EFT_CHECK_DONE, "listenAllTransactions")) return;

        log.info(" ({}) > KafkaListenerService | listenAllTransactions -> Data islenmek uzere alindi. UUID: {}, Type: {}", currentTime.get(), dto.getEventUUID(), dto.getTransactionType());

        dto.setStatus(TransactionStatus.FRAUD_REVIEW);
        dto.setStatusDescription(TransactionStatus.FRAUD_REVIEW.getDescription());
        sender.sendTransaction(dto.getEventUUID(), dto);

        markAsDone(dto.getEventUUID(), KafkaEventType.EFT_CHECK_DONE);
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
     */
    private void markAsDone(String eventUUID, KafkaEventType done) {
        eventRepository.save(KafkaEvent.builder()
                .eventId(eventUUID)
                .eventType(done.name())
                .createdAt(LocalDateTime.now())
                .build());
    }
}