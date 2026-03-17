package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.AuthServiceCreateUserTopicDto;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.repository.UserRepository;
import com.banking_microservices.user_service.service.UserService;
import com.banking_microservices.user_service.repository.KafkaEventRepository;
import com.banking_microservices.user_service.models.KafkaEvent;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaListenerService {

    private static final String AUTH_CREATE       = "USER_AUTH_CREATE";
    private static final String CREATE_SUCCESS    = "USER_CREATE_SUCCESS";
    private static final String TX_VALIDATE       = "USER_TX_VALIDATE";
    private static final String USERNAME_VALIDATE = "USER_USERNAME_VALIDATE";

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final UserService service;
    private final UserRepository repository;
    private final KafkaEventRepository eventRepository;

    public KafkaListenerService(UserService service, UserRepository repository, KafkaEventRepository eventRepository) {
        this.service = service;
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = "${kafka.topics.create-user.authservicelistener}")
    public void ListenAuthServiceTopic(String topicData) {
        AuthServiceCreateUserTopicDto dto = gson.fromJson(topicData, AuthServiceCreateUserTopicDto.class);
        if (dto == null || dto.getKeycloackUserUUID() == null) {
            log.warn("ListenAuthServiceTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getKeycloackUserUUID(), AUTH_CREATE)) {
            log.warn("ListenAuthServiceTopic - zaten islendi, atlaniyor: {}", dto.getKeycloackUserUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getKeycloackUserUUID())
                .eventType(AUTH_CREATE)
                .createdAt(LocalDateTime.now())
                .build());
        service.saveUser(dto);
    }

    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenCreateUserTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), CREATE_SUCCESS)) {
            log.warn("listenCreateUserTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(CREATE_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenTransactionTopic - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), TX_VALIDATE)) {
            log.warn("listenTransactionTopic - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(TX_VALIDATE)
                .createdAt(LocalDateTime.now())
                .build());
        service.transactionTopicMessageVerify(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUsernameValidation(String topic) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topic, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn("listenUsernameValidation - gecersiz mesaj alindi, atlaniyor.");
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), USERNAME_VALIDATE)) {
            log.warn("listenUsernameValidation - zaten islendi, atlaniyor: {}", dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(USERNAME_VALIDATE)
                .createdAt(LocalDateTime.now())
                .build());
        service.UsernameValidation(dto);
    }
}