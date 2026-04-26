package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.exception.EventUUIDAlreadyExists;
import com.banking_microservices.money_service.exception.EventUUIDNotFoundException;
import com.banking_microservices.money_service.models.SagaEvents;
import com.banking_microservices.money_service.repository.SagaEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SagaService {

    private SagaEventsRepository sagaEventsRepository;

    public void handleSagaEvent(SagaEvents sagaEvents){

        if(sagaEventsRepository.existsByUUID(sagaEvents.getKafkaEventUUID()) ){
        }

        if(sagaEvents.getKafkaEventUUID() == null){

        }


    }


    public void HandleException(){

    }


    //    public <T extends RuntimeException> void sendErrorAndThrow(KafkaTransactionTopicMessageDto dto, String description, T exception) {
    //        dto.setError(true);
    //        dto.setErrorDescription(description);
    //        kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
    //        log.error(" > TransactionErrorHandler | sendErrorAndThrow -> Hata Kafkaya iletildi. EventUUID: {}, Aciklama: {}", dto.getEventUUID(), description);
    //        throw exception;
    //    }
    public <T extends RuntimeException> void sendErrorAndThrow()

}
//|

//    private String UUID;
//
//    private String kafkaEventUUID;
//
//    private SagaStatus status;
//
//    private String errorDescripton;
//
//    private TransactionEntity transactionEntity;


//    CREATED,
//    PROCESS,
//    COMPLETED,
//    ERROR
//}
