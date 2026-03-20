package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.service.TransactionService;
import com.banking_microservices.money_service.service.UserMoneyService;
import com.banking_microservices.money_service.repository.KafkaEventRepository;
import com.banking_microservices.money_service.model.KafkaEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {

    private static final String EFT_PROCESS        = "MONEY_EFT_PROCESS";
    private static final String DEPOSIT_PROCESS    = "MONEY_DEPOSIT_PROCESS";
    private static final String WITHDRAW_PROCESS   = "MONEY_WITHDRAW_PROCESS";
    private static final String BLOCK_MONEY        = "MONEY_BLOCK_MONEY";
    private static final String USERNAME_VALIDATION = "MONEY_USERNAME_VALIDATION";

    private final TransactionService transactionService;
    private final UserMoneyService userMoneyService;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();
    private final KafkaSender kafkaSender;
    private final KafkaEventRepository eventRepository;
    private final Supplier<String> currentTime;

    public KafkaListenerService(TransactionService transactionService, UserMoneyService userMoneyService,
                                KafkaSender kafkaSender, KafkaEventRepository eventRepository, Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.eventRepository = eventRepository;
        this.currentTime = currentTime;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenTransactionTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), EFT_PROCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(EFT_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        transactionService.createTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenDepositTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (dto.getReceiverIban() == null) {
            log.warn(" ({}) > KafkaListenerService | listenDepositTopic -> receiverIban null, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), DEPOSIT_PROCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenDepositTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(DEPOSIT_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        try {
            userMoneyService.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendDepositSuccess(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Deposit tamamlandi ve transaction-service bilgilendirildi: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("Deposit islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            log.error(" ({}) > KafkaListenerService | listenDepositTopic -> Deposit basarisiz: {}", currentTime.get(), e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (dto.getSenderIban() == null || dto.getSenderIban().trim().isEmpty()) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawTopic -> senderIban bos/null, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            // Fallback: receiverIban dene (bazi servislerde withdraw de receiverIban olarak set ediliyor)
            if (dto.getReceiverIban() != null && !dto.getReceiverIban().trim().isEmpty()) {
                log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> receiverIban kullanilarak devam ediliyor: {}", currentTime.get(), dto.getReceiverIban());
                dto.setSenderIban(dto.getReceiverIban());
            } else {
                return;
            }
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), WITHDRAW_PROCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(WITHDRAW_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        try {
            userMoneyService.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendWithdrawSuccess(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Withdraw tamamlandi ve transaction-service bilgilendirildi: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("Withdraw islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            log.error(" ({}) > KafkaListenerService | listenWithdrawTopic -> Withdraw basarisiz: {}", currentTime.get(), e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoney(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenBlockMoney -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenBlockMoney -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), BLOCK_MONEY)) {
            log.warn(" ({}) > KafkaListenerService | listenBlockMoney -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(BLOCK_MONEY)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenBlockMoney -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topic) {
        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Metoda veri geldi. RawData: {}", currentTime.get(), topic);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), USERNAME_VALIDATION)) {
            log.warn(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(USERNAME_VALIDATION)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
    }
}