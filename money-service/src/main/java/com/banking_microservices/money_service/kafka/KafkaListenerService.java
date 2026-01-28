package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.service.TransactionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {
    private final TransactionService service;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public KafkaListenerService(TransactionService service) {
        this.service = service;
    }

    /*
    kafka.topics.transaction.listener ile isteği alıyor ve KafkaSender'de username-validation icin user-servıceye send edıyor ve burdada Ettıgı ıstegı okuyor.
    @since 2025.01.28
    @param kafka topicinden gelen veri KafkaTransactionTopicMessageDto turunde olmali.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData){
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        service.KafkaTransactionTopicService(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topic){
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);

    }

}
