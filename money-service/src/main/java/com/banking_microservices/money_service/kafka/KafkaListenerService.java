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

    /**
     * Ana transaction işlem noktası.
     * user-service kullanıcıyı doğruladıktan sonra bu topic'e mesaj gelir.
     * DEPOSIT → direkt bakiye yatırma → COMPLETED
     * WITHDRAW → direkt bakiye çekme → COMPLETED
     * TRANSFER → para blokeli akış (blockFunds → block-money topic)
     */
    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenUserValidatedTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenUserValidatedTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.TRANSACTION_PROCESS.name())) return;

        TransactionType txType = dto.getTransactionType();
        log.info(" ({}) > KafkaListenerService | listenUserValidatedTopic -> Data islenmek uzere alindi. Type: {}, EventUUID: {}", currentTime.get(), txType, dto.getEventUUID());

        if (txType == TransactionType.DEPOSIT) {
            handleDeposit(dto);
        } else if (txType == TransactionType.WITHDRAW) {
            handleWithdraw(dto);
        } else {
            // TRANSFER: validate, block funds
            handleTransfer(dto);
        }
    }

    // ─── DEPOSIT ─────────────────────────────────────────────────────────────

    private void handleDeposit(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleDeposit -> DEPOSIT baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            boolean noIban = isMissing(dto.getReceiverIban()) && isMissing(dto.getSenderIban());
            if (noIban && isMissing(dto.getSenderUserId())) {
                log.warn(" ({}) > KafkaListenerService | handleDeposit -> IBAN ve UserId null, isleme devam edilemiyor.", currentTime.get());
                sendError(dto, "Deposit icin IBAN veya UserId zorunludur.");
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
            sendError(dto, "Deposit islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── WITHDRAW ────────────────────────────────────────────────────────────

    private void handleWithdraw(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleWithdraw -> WITHDRAW baslatiliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            boolean noIban = isMissing(dto.getSenderIban()) && isMissing(dto.getReceiverIban());
            if (noIban && isMissing(dto.getSenderUserId())) {
                log.warn(" ({}) > KafkaListenerService | handleWithdraw -> IBAN ve UserId null, isleme devam edilemiyor.", currentTime.get());
                sendError(dto, "Withdraw icin IBAN veya UserId zorunludur.");
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
            sendError(dto, "Withdraw islemi basarisiz: " + e.getMessage());
        }
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    private void handleTransfer(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > KafkaListenerService | handleTransfer -> TRANSFER para bloke ediliyor. EventUUID: {}", currentTime.get(), dto.getEventUUID());
        try {
            transactionService.createTransaction(dto);
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | handleTransfer -> TRANSFER FAILED: {}", currentTime.get(), e.getMessage());
            sendError(dto, "Transfer islemi basarisiz: " + e.getMessage());
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

    private void sendError(KafkaTransactionTopicMessageDto dto, String reason) {
        dto.setError(true);
        dto.setErrorDescription(reason);
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