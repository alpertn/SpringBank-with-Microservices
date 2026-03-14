package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.AuthServiceCreateUserTopicDto;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.repository.UserRepository;
import com.banking_microservices.user_service.service.UserService;
import com.banking_microservices.user_service.repository.KafkaEventRepository;
import com.banking_microservices.user_service.models.KafkaEvent;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaListenerService {
    private final UserService service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaEventRepository eventRepository;

    @SneakyThrows
    private <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @KafkaListener(topics = "${kafka.topics.create-user.authservicelistener}")
    public void ListenAuthServiceTopic(String topicData) {
        AuthServiceCreateUserTopicDto dto = fromJson(topicData, AuthServiceCreateUserTopicDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getKeycloackUserUUID())
                .topicName("create-user.authservicelistener")
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .build());
        service.saveUser(dto);
    }

    public KafkaListenerService(UserService service, KafkaEventRepository eventRepository) {
        this.service = service;
        this.eventRepository = eventRepository;
    }

    //
    // Create User Topic Kafka
    //
    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .topicName("create-user.listener")
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .topicName("transaction.listener")
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .build());
        service.transactionTopicMessageVerify(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUsernameValidation(String topic) {
        KafkaTransactionTopicMessageDto dto = fromJson(topic, KafkaTransactionTopicMessageDto.class);
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .topicName("username-validation.listener")
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .build());
        service.UsernameValidation(dto);
    }
}
