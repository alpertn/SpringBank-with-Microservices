package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.enums.KafkaEventType;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.service.ProcessedEventStore;
import com.banking_microservices.user_service.service.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@Slf4j
public class KafkaListenerService {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    private final UserService userService;
    private final ProcessedEventStore processedEventStore;
    private final Supplier<String> currentTime;

    public KafkaListenerService(UserService userService,
                                ProcessedEventStore processedEventStore,
                                Supplier<String> currentTime) {
        this.userService = userService;
        this.processedEventStore = processedEventStore;
        this.currentTime = currentTime;
    }

    @KafkaListener(topics = "${kafka.topics.create-user.listener}")
    public void listenCreateUserTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            return;
        }
        if (!processedEventStore.markIfFirst(KafkaEventType.USER_CREATE_SUCCESS.name(), dto.getEventUUID())) {
            log.warn(" ({}) > KafkaListenerService | create-user success duplicate skipped: {}", currentTime.get(), dto.getEventUUID());
        }
    }

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        if (dto == null || dto.getEventUUID() == null) {
            return;
        }
        if (!processedEventStore.markIfFirst(KafkaEventType.USER_TX_VALIDATE.name(), dto.getEventUUID())) {
            log.warn(" ({}) > KafkaListenerService | transaction duplicate skipped: {}", currentTime.get(), dto.getEventUUID());
            return;
        }
        userService.transactionTopicMessageVerify(dto);
    }

}
