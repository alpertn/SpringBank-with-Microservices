package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.service.service;
import org.springframework.stereotype.Service;

@Service
public class KafkaListenerService {
    private final service service;

    public KafkaListenerService(service service) {
        this.service = service;
    }

    //GsonBuilder().serializeNulls()



}
