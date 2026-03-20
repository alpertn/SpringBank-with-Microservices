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
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private final UserService service;
    private final UserRepository repository;
    private final KafkaEventRepository eventRepository;

    private final java.util.function.Supplier<String> currentTime;

    public KafkaListenerService(UserService service, UserRepository repository, KafkaEventRepository eventRepository, java.util.function.Supplier<String> currentTime) {
        this.service = service;
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.currentTime = currentTime;
    }

    @KafkaListener(topics = "${kafka.topics.create-user.authservicelistener}")
    public void ListenAuthServiceTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | ListenAuthServiceTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        AuthServiceCreateUserTopicDto dto = gson.fromJson(topicData, AuthServiceCreateUserTopicDto.class);
        if (dto == null || dto.getKeycloackUserUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | ListenAuthServiceTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getKeycloackUserUUID(), AUTH_CREATE)) {
            log.warn(" ({}) > KafkaListenerService | ListenAuthServiceTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getKeycloackUserUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getKeycloackUserUUID())
                .eventType(AUTH_CREATE)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | ListenAuthServiceTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        service.saveUser(dto);
    }

    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenCreateUserTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenCreateUserTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), CREATE_SUCCESS)) {
            log.warn(" ({}) > KafkaListenerService | listenCreateUserTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(CREATE_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenCreateUserTopic -> Islem basarili, eventi kaydettik. Dto: {}", currentTime.get(), gson.toJson(dto));
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), TX_VALIDATE)) {
            log.warn(" ({}) > KafkaListenerService | listenTransactionTopic -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(TX_VALIDATE)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenTransactionTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        service.transactionTopicMessageVerify(dto);
    }

    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUsernameValidation(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenUsernameValidation -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            log.warn(" ({}) > KafkaListenerService | listenUsernameValidation -> Gecersiz mesaj alindi, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }
        if (eventRepository.existsByEventIdAndEventType(dto.getEventUUID(), USERNAME_VALIDATE)) {
            log.warn(" ({}) > KafkaListenerService | listenUsernameValidation -> Zaten islendi, atlaniyor: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        eventRepository.save(KafkaEvent.builder()
                .eventId(dto.getEventUUID())
                .eventType(USERNAME_VALIDATE)
                .createdAt(LocalDateTime.now())
                .build());
        log.info(" ({}) > KafkaListenerService | listenUsernameValidation -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));
        service.UsernameValidation(dto);
    }
}