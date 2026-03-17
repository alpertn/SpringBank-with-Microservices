package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.service.TransactionService;
import com.banking_microservices.money_service.service.UserMoneyService;
import com.banking_microservices.money_service.repository.KafkaEventRepository;
import com.banking_microservices.money_service.model.KafkaEvent;
import java.time.LocalDateTime;
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
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();
    private final KafkaSender kafkaSender;
    private final KafkaEventRepository eventRepository;

    public KafkaListenerService(TransactionService transactionService, UserMoneyService userMoneyService,
                                KafkaSender kafkaSender, KafkaEventRepository eventRepository) {
        this.transactionService = transactionService;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenTransactionTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), EFT_PROCESS)) {
            log.warn("listenTransactionTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(EFT_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        transactionService.createTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenDepositTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (dto.getReceiverIban() == null) {
            log.warn("listenDepositTopic - receiverIban null, atlaniyor. eventUUID: {}", dto.getEventUUID());
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), DEPOSIT_PROCESS)) {
            log.warn("listenDepositTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(DEPOSIT_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        try {
            userMoneyService.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendDepositSuccess(dto.getEventUUID(), dto);
            log.info("Deposit tamamlandi ve transaction-service bilgilendirildi: {}", dto.getEventUUID());
        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("Deposit islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            log.error("Deposit basarisiz: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawTopic(String topicData) {
        log.info("listenWithdrawTopic - RAW JSON: {}", topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenWithdrawTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (dto.getSenderIban() == null || dto.getSenderIban().trim().isEmpty()) {
            log.warn("listenWithdrawTopic - senderIban bos/null, atlaniyor. eventUUID: {}, senderIban: '{}'",
                    dto.getEventUUID(), dto.getSenderIban());
            // Fallback: receiverIban dene (bazi servislerde withdraw de receiverIban olarak set ediliyor)
            if (dto.getReceiverIban() != null && !dto.getReceiverIban().trim().isEmpty()) {
                log.info("listenWithdrawTopic - receiverIban kullanilarak devam ediliyor: {}", dto.getReceiverIban());
                dto.setSenderIban(dto.getReceiverIban());
            } else {
                return;
            }
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), WITHDRAW_PROCESS)) {
            log.warn("listenWithdrawTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(WITHDRAW_PROCESS)
                .createdAt(LocalDateTime.now())
                .build());
        try {
            userMoneyService.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());
            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            kafkaSender.sendWithdrawSuccess(dto.getEventUUID(), dto);
            log.info("Withdraw tamamlandi ve transaction-service bilgilendirildi: {}", dto.getEventUUID());
        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("Withdraw islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            log.error("Withdraw basarisiz: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoney(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenBlockMoney - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), BLOCK_MONEY)) {
            log.warn("listenBlockMoney - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(BLOCK_MONEY)
                .createdAt(LocalDateTime.now())
                .build());
        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topic) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenUserValidationTopicOnUserService - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), USERNAME_VALIDATION)) {
            log.warn("listenUserValidationTopicOnUserService - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(USERNAME_VALIDATION)
                .createdAt(LocalDateTime.now())
                .build());
    }
}