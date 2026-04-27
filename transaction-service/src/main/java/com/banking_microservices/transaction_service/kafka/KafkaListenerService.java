package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.ValueNotFoundException;
import com.banking_microservices.transaction_service.model.SagaEvents;
import com.banking_microservices.transaction_service.model.TransactionEntity;
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
            .setPrettyPrinting()
            .create();

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final Supplier<String> currentTime;

    public KafkaListenerService(TransactionService transactionService,
                                TransactionRepository transactionRepository,
                                Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.currentTime = currentTime;
    }

    // ─── Listeners ──────────────────────────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Metoda veri geldi. RawData:\n{}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenErrorTopic");
        if (dto == null) return;

        log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Data islenmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(dto));

        // DÜZELTME: Error mesajlarını her zaman işle. Önceden isDuplicate kontrolü yapılıyordu
        // ve status aynıysa (örn. hem entity hem dto BLOCK_MONEY ise) mesaj atlanıyordu.
        // Bu, başarısız transferlerin sonsuza dek BLOCK_MONEY'de kalmasına neden oluyordu.
        var entityOpt = transactionRepository.findByEventId(dto.getEventUUID());
        if (entityOpt.isPresent()) {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Error status guncellendi: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } else {
            transactionService.saveTransaction(dto);
            log.info(" ({}) > KafkaListenerService | listenErrorTopic -> Yeni error entity kaydedildi: {}", currentTime.get(), dto.getEventUUID());
        }
    }

    // DÜZELTME: Bu listener farklı bir consumer group kullanmalı.
    // Önceden aynı group-id (transaction-service-group) ile çalışıyordu ve
    // topicPattern regex'i diğer listener'ların topic'lerini de kapsadığı için
    // Kafka partition'ları bazen bu listener'a veriyordu. Sonuç: blockmoney,
    // user-validation ve result mesajları asıl işleyen listener'lara ulaşmıyordu
    // ve transaction statüsü hep CREATED kalıyordu.
    @KafkaListener(topicPattern = "${kafka.topics.transaction.logger.listener}", groupId = "transaction-service-logger-group")
    public void listenAllTransactionTopics(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> Metoda veri geldi. RawData:\n{}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenAllTransactionTopics");
        if (dto == null) return;

        log.info(" ({}) > KafkaListenerService | listenAllTransactionTopics -> event: {} status: {}, Dto:\n{}", currentTime.get(), dto.getEventUUID(), dto.getStatus(), gson.toJson(dto));

        // Sadece loglama — iş mantığı buraya eklenmemeli.
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoneyTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenBlockMoneyTopic -> Metoda veri geldi. RawData:\n{}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenBlockMoneyTopic");
        if (dto == null) return;

        if (isDuplicate(dto, false, "listenBlockMoneyTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenBlockMoneyTopic -> Data islenmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(dto));

        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenBlockMoneyTopic -> BLOCK_MONEY status guncellendi: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenBlockMoneyTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.user-validation.listener}")
    public void listenUserValidationSuccessTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenUserValidationSuccessTopic -> Metoda veri geldi. RawData:\n{}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenUserValidationSuccessTopic");
        if (dto == null) return;

        if (isDuplicate(dto, false, "listenUserValidationSuccessTopic")) return;

        log.info(" ({}) > KafkaListenerService | listenUserValidationSuccessTopic -> Data islenmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(dto));

        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenUserValidationSuccessTopic -> Validation status guncellendi: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenUserValidationSuccessTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.result.listener}")
    public void listenTransactionResultTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionResultTopic -> Metoda veri geldi. RawData:\n{}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData, "listenTransactionResultTopic");
        if (dto == null) return;

        // COMPLETED/FAILED sonuçları için güçlendirilmiş kontrolü:
        // isDuplicate yerine direkt entity kontrolü yapıyoruz — entity yoksa WARN, varsa status güncelle.
        if (dto.getStatus() != null) {
            java.util.Optional<com.banking_microservices.transaction_service.model.TransactionEntity> opt =
                transactionRepository.findByEventId(dto.getEventUUID());
            if (opt.isEmpty()) {
                log.warn(" ({}) > KafkaListenerService | listenTransactionResultTopic -> Entity bulunamadi! EventUUID: {}. Status guncelleme atlanacak.", currentTime.get(), dto.getEventUUID());
                return;
            }
            if (opt.get().getStatus() == dto.getStatus()) {
                log.info(" ({}) > KafkaListenerService | listenTransactionResultTopic -> Zaten bu statude, atlaniyor: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
                return;
            }
        }

        log.info(" ({}) > KafkaListenerService | listenTransactionResultTopic -> Status guncelleniyor: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());

        try {
            transactionService.updateTransactionStatus(dto);
            log.info(" ({}) > KafkaListenerService | listenTransactionResultTopic -> Transaction result status GUNCELLENDI: {} -> {}", currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenTransactionResultTopic -> updateTransactionStatus hatasi: {}", currentTime.get(), e.getMessage(), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private KafkaTransactionTopicMessageDto parseMessage(String topicData, String method) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | {} -> Gecersiz mesaj alindi, atlaniyor.", currentTime.get(), method);
            return null;
        }
        return dto;
    }

    private boolean isDuplicate(KafkaTransactionTopicMessageDto dto, boolean isCreation, String method) {
        if (isCreation) {
            boolean exists = transactionRepository.existsByEventId(dto.getEventUUID());
            if (exists) log.warn(" ({}) > KafkaListenerService | {} -> Zaten islendi (Created), atlaniyor: {}", currentTime.get(), method, dto.getEventUUID());
            return exists;
        } else {
            java.util.Optional<TransactionEntity> opt = transactionRepository.findByEventId(dto.getEventUUID());
            if (opt.isPresent() && opt.get().getStatus() == dto.getStatus()) {
                log.warn(" ({}) > KafkaListenerService | {} -> Zaten bu statüde güncellendi ({}), atlaniyor: {}", currentTime.get(), method, dto.getStatus(), dto.getEventUUID());
                return true;
            }
            return false;
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.saga.listener}")
    public void listenSagaTopic(String data) {
        log.info(" ({}) > KafkaListenerService | listenSagaTopic -> Metoda veri geldi. RawData:\n{}", currentTime.get(), data);

        if (data == null || data.isBlank()) {
            log.warn(" ({}) > KafkaListenerService | listenSagaTopic -> Gecersiz (bos) mesaj alindi, atlaniyor.", currentTime.get());
            throw new ValueNotFoundException("Value Not Found On Saga Kafka Topic Listener. data is null or empty");
        }

        SagaEvents model = gson.fromJson(data, SagaEvents.class);
        if (model == null || model.getUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenSagaTopic -> Parse edilemedi veya UUID null, atlaniyor. RawData:\n{}", currentTime.get(), data);
            throw new ValueNotFoundException("Value Not Found On Saga Kafka Topic Listener. model: " + data);
        }

        log.info(" ({}) > KafkaListenerService | listenSagaTopic -> Saga event islenmek uzere alindi. UUID: {}, Status: {}", currentTime.get(), model.getUUID(), model.getStatus());

        try {
            transactionService.updateSagaEventStatus(model);
            log.info(" ({}) > KafkaListenerService | listenSagaTopic -> Saga event status guncellendi. UUID: {}, Status: {}", currentTime.get(), model.getUUID(), model.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenSagaTopic -> updateSagaEventStatus hatasi. UUID: {}, Hata: {}", currentTime.get(), model.getUUID(), e.getMessage(), e);
        }
    }
}