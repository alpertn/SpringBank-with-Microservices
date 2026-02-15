package com.banking_microservices.auth_service.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaSender {


    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

}
