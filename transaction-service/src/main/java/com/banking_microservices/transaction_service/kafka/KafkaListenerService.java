package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.service.TransactionService;
import org.springframework.stereotype.Service;

@Service
public class KafkaListenerService {
    private final TransactionService TransactionService;

    public KafkaListenerService(TransactionService TransactionService) {
        this.TransactionService = TransactionService;
    }

    //GsonBuilder().serializeNulls()



}
