package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.AuthServiceCreateUserTopicDto;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.repository.UserRepository;
import com.banking_microservices.user_service.service.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaListenerService {
    private final UserService service;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final UserRepository repository;

    @KafkaListener(topics = "${kafka.topics.create-user.authservicelistener}")
    public void ListenAuthServiceTopic(String topicData) {
        AuthServiceCreateUserTopicDto dto = gson.fromJson(topicData, AuthServiceCreateUserTopicDto.class);

    }

    public KafkaListenerService(UserService service, UserRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    //
    // Create User Topic Kafka
    //
    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUsernameValidation(String topic) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);
        service.UsernameValidation(dto);
    }
}
