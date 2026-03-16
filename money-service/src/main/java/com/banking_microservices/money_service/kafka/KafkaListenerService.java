package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
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
    private final TransactionService service;
    private final UserMoneyService userMoneyService;
    private final Gson gson = new GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(LocalDateTime.class,
            (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                LocalDateTime.parse(json.getAsString()))
        .create();
    private final KafkaSender kafkaSender;
    private final TransactionService transactionService;
    private final KafkaEventRepository eventRepository;

    public KafkaListenerService(TransactionService service, UserMoneyService userMoneyService,
            KafkaSender kafkaSender, TransactionService transactionService, KafkaEventRepository eventRepository) {
        this.service = service;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.transactionService = transactionService;
        this.eventRepository = eventRepository;
    }

    /*
     * kafka.topics.transaction.listener ile isteği alıyor ve KafkaSender'de
     * username-validation icin user-servıceye send edıyor ve burdada Ettıgı ıstegı
     * okuyor.
     * 
     * @since 2025.01.28
     * 
     * @param kafka topicinden gelen veri KafkaTransactionTopicMessageDto turunde
     * olmali.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        service.createTransaction(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        userMoneyService.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());
    }

    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        userMoneyService.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoney(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topic) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .createdAt(LocalDateTime.now())
                .build());
    }

}
