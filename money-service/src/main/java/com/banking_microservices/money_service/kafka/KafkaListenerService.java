package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.KafkaEventType;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.dto.enums.TransactionType;
import com.banking_microservices.money_service.service.TransactionService;
import com.banking_microservices.money_service.service.UserMoneyService;
import com.banking_microservices.money_service.service.helper.IdempotencyGuard;
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

    private final TransactionService transactionService;
    private final UserMoneyService userMoneyService;
    private final KafkaSender kafkaSender;
    private final IdempotencyGuard idempotencyGuard;
    private final Supplier<String> currentTime;

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    public KafkaListenerService(TransactionService transactionService,
                                UserMoneyService userMoneyService,
                                KafkaSender kafkaSender,
                                IdempotencyGuard idempotencyGuard,
                                Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.idempotencyGuard = idempotencyGuard;
        this.currentTime = currentTime;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ADIM 1 — created.v1 dinle
    //   DEPOSIT  → direkt para yatır → COMPLETED → result topic
    //   WITHDRAW → direkt para çek   → COMPLETED → result topic
    //   TRANSFER → parayı BLOKE et → block-money.success.v1
    //              (user-service → fraud-service → fraud-checked.v1 → ADIM 4)
    //
    //   NOT: fraud-service artık user-validation.success.v1 dinliyor.
    //   DEPOSIT/WITHDRAW için user-validation gönderilmez, dolayısıyla
    //   fraud-service bu tipleri görmez. Direkt işlenmeleri doğrudur.
    // ═══════════════════════════════════════════════════════════════════════════════

    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenCreatedTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenCreatedTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.TRANSACTION_PROCESS.name())) return;

        TransactionType txType = dto.getTransactionType();
        log.info(" ({}) > KafkaListenerService | listenCreatedTopic -> Data islenmek uzere alindi. Type: {}, EventUUID: {}", currentTime.get(), txType, dto.getEventUUID());

        if (txType == TransactionType.DEPOSIT) {
            // DEPOSIT: fraud-service bu tiple muhatap değil (user-validation.success.v1 dinliyor).
            // Direkt para yatır.
            handleDeposit(dto);
        } else if (txType == TransactionType.WITHDRAW) {
            // WITHDRAW: Aynı sebepten direkt para çek.
            handleWithdraw(dto);
        } else if (txType == TransactionType.TRANSFER) {
            // TRANSFER ADIM 1: Sadece BLOKE et.
            // Sonrası: block-money.success.v1 → user-service → user-validation.success.v1
            //          → fraud-service → fraud-checked.v1 → listenFraudCheckedTopic (ADIM 4)
            handleBlockMoney(dto);
        } else {
            log.warn(" ({}) > KafkaListenerService | listenCreatedTopic -> Bilinmeyen transaction tipi: {}, EventUUID: {}", currentTime.get(), txType, dto.getEventUUID());
        }
    }



    // ═══════════════════════════════════════════════════════════════════════
    // ADIM 4 — fraud-checked.v1 dinle (TRANSFER için)
    //   TRANSFER → gerçek para hareketi: withdrawBlocked + deposit → COMPLETED
    //   DEPOSIT / WITHDRAW → bu listener'da işlenmez (zaten ADIM 1'de bitti)
    // ═══════════════════════════════════════════════════════════════════════

    @KafkaListener(topics = "${kafka.topics.transaction.transferexecute.listener}")
    public void listenFraudCheckedTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        TransactionType txType = dto.getTransactionType();

        // DEPOSIT ve WITHDRAW zaten listenCreatedTopic'te işlendi — burada atla
        if (txType == TransactionType.DEPOSIT || txType == TransactionType.WITHDRAW) {
            log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> {}, zaten ADIM 1'de islendi, atlaniyor. UUID: {}",
                    currentTime.get(), txType, dto.getEventUUID());
            return;
        }

        if (txType != TransactionType.TRANSFER) {
            log.warn(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Bilinmeyen tip: {}, UUID: {}", currentTime.get(), txType, dto.getEventUUID());
            return;
        }

        // TRANSFER için idempotency kontrolü
        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.FRAUD_CHECKED_EFT.name())) {
            log.warn(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Zaten islendi, atlaniyor. UUID: {}", currentTime.get(), dto.getEventUUID());
            return;
        }

        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> TRANSFER execute baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            transactionService.createTransaction(dto);
            log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> TRANSFER COMPLETED. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> TRANSFER EXECUTE FAILED: {}", currentTime.get(), e.getMessage());
            sendError(dto, TransactionType.TRANSFER, "Transfer islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── DEPOSIT ─────────────────────────────────────────────────────────────

    private void handleDeposit(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleDeposit -> DEPOSIT baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            boolean noIban = isMissing(dto.getReceiverIban()) && isMissing(dto.getSenderIban());
            if (noIban && isMissing(dto.getSenderUserId())) {
                log.warn(" ({}) > KafkaListenerService | handleDeposit -> IBAN ve UserId null, isleme devam edilemiyor.", currentTime.get());
                sendError(dto, TransactionType.DEPOSIT, "Deposit icin IBAN veya UserId zorunludur.");
                return;
            }
            if (!noIban) {
                String targetIban = !isMissing(dto.getReceiverIban()) ? dto.getReceiverIban() : dto.getSenderIban();
                log.info(" ({}) > KafkaListenerService | handleDeposit -> IBAN ile yatiriliyor: {}", currentTime.get(), targetIban);
                userMoneyService.depositMoneyByIban(targetIban, dto.getMoney());
            } else {
                log.info(" ({}) > KafkaListenerService | handleDeposit -> UserId ile yatiriliyor: {}", currentTime.get(), dto.getSenderUserId());
                userMoneyService.depositMoneyByUserId(dto.getSenderUserId(), dto.getMoney());
            }
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendResult(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | handleDeposit -> DEPOSIT COMPLETED. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | handleDeposit -> DEPOSIT FAILED: {}", currentTime.get(), e.getMessage());
            sendError(dto, TransactionType.DEPOSIT, "Deposit islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── WITHDRAW ────────────────────────────────────────────────────────────

    private void handleWithdraw(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleWithdraw -> WITHDRAW baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            boolean noIban = isMissing(dto.getSenderIban()) && isMissing(dto.getReceiverIban());
            if (noIban && isMissing(dto.getSenderUserId())) {
                log.warn(" ({}) > KafkaListenerService | handleWithdraw -> IBAN ve UserId null, isleme devam edilemiyor.", currentTime.get());
                sendError(dto, TransactionType.WITHDRAW, "Withdraw icin IBAN veya UserId zorunludur.");
                return;
            }
            if (!noIban) {
                String targetIban = !isMissing(dto.getSenderIban()) ? dto.getSenderIban() : dto.getReceiverIban();
                log.info(" ({}) > KafkaListenerService | handleWithdraw -> IBAN ile cekiliyor: {}", currentTime.get(), targetIban);
                userMoneyService.withdrawMoneyByIban(targetIban, dto.getMoney());
            } else {
                log.info(" ({}) > KafkaListenerService | handleWithdraw -> UserId ile cekiliyor: {}", currentTime.get(), dto.getSenderUserId());
                userMoneyService.withdrawMoneyByUserId(dto.getSenderUserId(), dto.getMoney());
            }
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendResult(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | handleWithdraw -> WITHDRAW COMPLETED. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | handleWithdraw -> WITHDRAW FAILED: {}", currentTime.get(), e.getMessage());
            sendError(dto, TransactionType.WITHDRAW, "Withdraw islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── TRANSFER ADIM 1: BlockMoney ────────────────────────────────────────

    private void handleBlockMoney(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleBlockMoney -> TRANSFER icin BlockMoney baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            // Sender IBAN resolve + bakiye doğrulama + parayı bloke et
            // Başarılı olursa block-money.success.v1 topic'e gönderir (BlockMoneyService içinde)
            transactionService.KafkaTransactionTopicBlockMoney(dto);
            log.info(" ({}) > KafkaListenerService | handleBlockMoney -> BLOCK_MONEY tamamlandi. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | handleBlockMoney -> BlockMoney FAILED: {}", currentTime.get(), e.getMessage());
            sendError(dto, TransactionType.TRANSFER, "Transfer bloke islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private KafkaTransactionTopicMessageDto parseMessage(String topicData) {
        try {
            KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
            if (dto == null || dto.getEventUUID() == null) {
                log.warn(" ({}) > KafkaListenerService | parseMessage -> Gecersiz mesaj alindi, atlaniyor. RawData: {}", currentTime.get(), topicData);
                return null;
            }
            return dto;
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | parseMessage -> JSON parse hatasi! RawData: {}, Hata: {}", currentTime.get(), topicData, e.getMessage());
            return null;
        }
    }

    /**
     * İşlem tipine göre doğru hata statüsünü set ederek error topic'e gönderir.
     * DEPOSIT  → DEPOSIT_FAILED
     * WITHDRAW → WITHDRAW_FAILED
     * TRANSFER → FAILED
     */
    private void sendError(KafkaTransactionTopicMessageDto dto, TransactionType txType, String reason) {
        dto.setError(true);
        dto.setErrorDescription(reason);
        if (txType == TransactionType.DEPOSIT) {
            dto.setStatus(TransactionStatus.DEPOSIT_FAILED);
            dto.setStatusDescription(TransactionStatus.DEPOSIT_FAILED.getDescription());
        } else if (txType == TransactionType.WITHDRAW) {
            dto.setStatus(TransactionStatus.WITHDRAW_FAILED);
            dto.setStatusDescription(TransactionStatus.WITHDRAW_FAILED.getDescription());
        } else {
            dto.setStatus(TransactionStatus.FAILED);
            dto.setStatusDescription(TransactionStatus.FAILED.getDescription());
        }
        try {
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
        } catch (Exception ex) {
            log.error(" ({}) > KafkaListenerService | sendError -> Error topicine gonderilemedi: {}", currentTime.get(), ex.getMessage());
        }
    }

    private boolean isMissing(String value) {
        return value == null || value.trim().isEmpty();
    }
}