package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.models.KafkaLastActivity;
import com.banking_microservices.money_service.repository.KafkaLastActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final KafkaLastActivityRepository kafkaLastActivityRepository;
    private final Supplier<String> currentTime;

    public boolean isDuplicateOrRegister(String eventUUID, String eventType) {
        if (kafkaLastActivityRepository.existsByEventUUIDAndEventType(eventUUID, eventType)) {
            log.warn(" ({}) > IdempotencyGuard | isDuplicateOrRegister -> Event zaten islendi, atlaniyor. EventUUID: {}, EventType: {}", currentTime.get(), eventUUID, eventType);
            return true;
        }

        try {
            kafkaLastActivityRepository.save(KafkaLastActivity.builder()
                    .eventUUID(eventUUID)
                    .eventType(eventType)
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info(" ({}) > IdempotencyGuard | isDuplicateOrRegister -> EventUUID kaydedildi. EventUUID: {}, EventType: {}", currentTime.get(), eventUUID, eventType);
        } catch (Exception e) {
            // kayit basarisiz olsa da islemi durdurmuyoruz. race condition ihtimaline karsi devam etmek daha guvenli.
            log.error(" ({}) > IdempotencyGuard | isDuplicateOrRegister -> EventUUID kaydedilemedi! EventUUID: {}, Hata: {}", currentTime.get(), eventUUID, e.getMessage());
        }

        return false;
    }
}
