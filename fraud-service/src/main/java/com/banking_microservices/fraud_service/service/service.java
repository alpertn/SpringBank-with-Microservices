package com.banking_microservices.fraud_service.service;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.kafka.KafkaSenderService;
import org.springframework.stereotype.Service;

@Service
public class service {
    private KafkaSenderService kafkaSender;

    public service(KafkaSenderService kafkaSender) {
        this.kafkaSender = kafkaSender;
    }

    public void send(KafkaTransactionTopicMessageDto requestDto){
        kafkaSender.sendTransaction(requestDto.getEventUUID(),requestDto);
    }


}
