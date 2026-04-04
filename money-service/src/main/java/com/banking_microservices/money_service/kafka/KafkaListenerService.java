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

    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenFraudCheckedTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.TRANSACTION_PROCESS.name())) return;

        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Data islenmek uzere alindi. Type: {}, Dto: \n{}", currentTime.get(), dto.getTransactionType(), gson.toJson(dto));

        // Sadece TRANSFER bu topic'ten islemden gecmeli
        transactionService.createTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoneyFromTxService(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.BLOCK_MONEY.name())) return;

        log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> Data islenmek uzere alindi. Type: {}, Dto: \n{}", currentTime.get(), dto.getTransactionType(), gson.toJson(dto));

        TransactionType txType = dto.getTransactionType();

        // DEPOSIT: bloke atlanır, direkt bakiye artırılır, COMPLETED gönderilir
        if (txType == TransactionType.DEPOSIT) {
            log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> DEPOSIT icin bloke atlaniyor, direkt yatiriliyor.", currentTime.get());
            try {
                boolean noIban = isMissing(dto.getReceiverIban()) && isMissing(dto.getSenderIban());
                if (noIban && isMissing(dto.getSenderUserId())) {
                    log.warn(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | DEPOSIT -> Iban ve UserId null, atlaniyor.", currentTime.get());
                    return;
                }
                if (!noIban) {
                    String targetIban = !isMissing(dto.getReceiverIban()) ? dto.getReceiverIban() : dto.getSenderIban();
                    userMoneyService.depositMoneyByIban(targetIban, dto.getMoney());
                } else {
                    userMoneyService.depositMoneyByUserId(dto.getSenderUserId(), dto.getMoney());
                }
                dto.setStatus(TransactionStatus.COMPLETED);
                dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
                kafkaSender.sendResult(dto.getEventUUID(), dto);
                log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | DEPOSIT -> Tamamlandi: {}", currentTime.get(), dto.getEventUUID());
            } catch (Exception e) {
                log.error(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | DEPOSIT -> Basarisiz: {}", currentTime.get(), e.getMessage());
                dto.setError(true);
                dto.setErrorDescription("Deposit islemi basarisiz: " + e.getMessage());
                kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            }
            return;
        }

        // WITHDRAW: bloke atlanır, direkt bakiye düşürülür, COMPLETED gönderilir
        if (txType == TransactionType.WITHDRAW) {
            log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> WITHDRAW icin bloke atlaniyor, direkt cekilliyor.", currentTime.get());
            try {
                boolean noIban = isMissing(dto.getSenderIban()) && isMissing(dto.getReceiverIban());
                if (noIban && isMissing(dto.getSenderUserId())) {
                    log.warn(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | WITHDRAW -> Iban ve UserId null, atlaniyor.", currentTime.get());
                    return;
                }
                if (!noIban) {
                    String targetIban = !isMissing(dto.getSenderIban()) ? dto.getSenderIban() : dto.getReceiverIban();
                    userMoneyService.withdrawMoneyByIban(targetIban, dto.getMoney());
                } else {
                    userMoneyService.withdrawMoneyByUserId(dto.getSenderUserId(), dto.getMoney());
                }
                dto.setStatus(TransactionStatus.COMPLETED);
                dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
                kafkaSender.sendResult(dto.getEventUUID(), dto);
                log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | WITHDRAW -> Tamamlandi: {}", currentTime.get(), dto.getEventUUID());
            } catch (Exception e) {
                log.error(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService | WITHDRAW -> Basarisiz: {}", currentTime.get(), e.getMessage());
                dto.setError(true);
                dto.setErrorDescription("Withdraw islemi basarisiz: " + e.getMessage());
                kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            }
            return;
        }

        // Yalnızca TRANSFER icin gercek bloke islemi yapilir
        log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> TRANSFER icin para bloke ediliyor.", currentTime.get());
        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.USERNAME_VALIDATION.name())) return;

        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Data islenmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));

        // TODO: username validation is mantigi buraya eklenecek
    }

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

    private boolean isMissing(String value) {
        return value == null || value.trim().isEmpty();
    }
}