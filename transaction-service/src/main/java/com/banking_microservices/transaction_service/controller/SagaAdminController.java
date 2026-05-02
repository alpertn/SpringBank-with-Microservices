package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.model.SagaEvents;
import com.banking_microservices.transaction_service.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Saga event durum kontrol ve yönetim controller'ı.
 * Gateway SecurityConfig: /api/transaction-service/v1/saga/** → hasRole("ADMIN")
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction-service/v1/saga")
public class SagaAdminController {

    private final TransactionService transactionService;
    private final Supplier<String> currentTime;

    public SagaAdminController(TransactionService transactionService, Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.currentTime = currentTime;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/transaction-service/v1/saga/all
    // Tüm saga event'lerini listeler
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<SagaEvents>> getAllSagaEvents() {
        log.info(" ({}) > SagaAdminController | getAllSagaEvents -> Istek alindi.", currentTime.get());
        List<SagaEvents> events = transactionService.getAllSagaEvents();
        log.info(" ({}) > SagaAdminController | getAllSagaEvents -> {} adet saga event donduruluyor.", currentTime.get(), events.size());
        return ResponseEntity.ok(events);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/transaction-service/v1/saga/status?uuid=...
    // Saga UUID ile durum sorgulama
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<SagaEvents> getSagaStatusByUUID(@RequestParam String uuid) {
        log.info(" ({}) > SagaAdminController | getSagaStatusByUUID -> Istek alindi. UUID: {}", currentTime.get(), uuid);
        SagaEvents event = transactionService.getSagaEventByUUID(uuid);
        log.info(" ({}) > SagaAdminController | getSagaStatusByUUID -> Bulundu. UUID: {}, Status: {}", currentTime.get(), event.getUUID(), event.getStatus());
        return ResponseEntity.ok(event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/transaction-service/v1/saga/status/by-transaction?eventUUID=...
    // Transaction event UUID (kafkaEventUUID) ile saga durumu sorgulama
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/status/by-transaction")
    public ResponseEntity<SagaEvents> getSagaStatusByTransactionEventUUID(@RequestParam String eventUUID) {
        log.info(" ({}) > SagaAdminController | getSagaStatusByTransactionEventUUID -> Istek alindi. EventUUID: {}", currentTime.get(), eventUUID);
        SagaEvents event = transactionService.getSagaEventByKafkaEventUUID(eventUUID);
        log.info(" ({}) > SagaAdminController | getSagaStatusByTransactionEventUUID -> Bulundu. UUID: {}, Status: {}, KafkaEventUUID: {}",
                currentTime.get(), event.getUUID(), event.getStatus(), event.getKafkaEventUUID());
        return ResponseEntity.ok(event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/transaction-service/v1/saga/create?eventUUID=...
    // Var olan bir transaction için manuel Saga başlatır (admin tetiklemesi)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> createSagaEvent(@RequestParam String eventUUID) {
        log.info(" ({}) > SagaAdminController | createSagaEvent -> Istek alindi. EventUUID: {}", currentTime.get(), eventUUID);

        if (eventUUID == null || eventUUID.isBlank()) {
            log.warn(" ({}) > SagaAdminController | createSagaEvent -> EventUUID bos!", currentTime.get());
            return ResponseEntity.badRequest().body(Map.of("error", "eventUUID bos olamaz."));
        }

        transactionService.createSagaEvent(eventUUID);
        log.info(" ({}) > SagaAdminController | createSagaEvent -> Saga event olusturuldu. EventUUID: {}", currentTime.get(), eventUUID);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Saga event olusturuldu ve Kafka'ya gonderildi.",
                "eventUUID", eventUUID
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/transaction-service/v1/saga/exists?eventUUID=...
    // Transaction event UUID için saga kaydının var olup olmadığını kontrol et
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> sagaExists(@RequestParam String eventUUID) {
        log.info(" ({}) > SagaAdminController | sagaExists -> Istek alindi. EventUUID: {}", currentTime.get(), eventUUID);
        boolean exists = transactionService.sagaExistsByKafkaEventUUID(eventUUID);
        log.info(" ({}) > SagaAdminController | sagaExists -> EventUUID: {}, Exists: {}", currentTime.get(), eventUUID, exists);
        return ResponseEntity.ok(Map.of(
                "eventUUID", eventUUID,
                "exists", exists
        ));
    }
}
