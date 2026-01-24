package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.service.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {

    private final service service;

    public KafkaListenerService(service service) {
        this.service = service;
    }

    @KafkaListener(topics = "CreateUser-Topic")
    public void CreateUserListener(String userId){
        log.info("CreateUser-Topic'den mesaj geldi {} ", userId);
        service.generateUser(userId);
    }
}
