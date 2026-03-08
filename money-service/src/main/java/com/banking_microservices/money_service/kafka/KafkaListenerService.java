package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.KafkaCreateUserException;
import com.banking_microservices.money_service.service.TransactionService;
import com.banking_microservices.money_service.service.UserMoneyService;
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
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final KafkaSender kafkaSender;
    private final TransactionService transactionService;

    public KafkaListenerService(TransactionService service, UserMoneyService userMoneyService,
            KafkaSender kafkaSender, TransactionService transactionService) {
        this.service = service;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.transactionService = transactionService;
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
        service.KafkaTransactionTopicService(dto);
    }

    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoney(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topic) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);

    }

    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopicOnUserService(String topic) {
        try {
            userMoneyService.generateUser(topic);
        } catch (Exception e) {
            throw new KafkaCreateUserException("An Error In Create User On Kafka Topic Request");
        }

    }

}
