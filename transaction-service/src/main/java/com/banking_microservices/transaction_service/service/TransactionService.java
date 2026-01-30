package com.banking_microservices.transaction_service.service;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.*;
import com.banking_microservices.transaction_service.kafka.KafkaSender;
import com.banking_microservices.transaction_service.model.transaction;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository TransactionRepository;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final KafkaSender kafkaSender;
    private String newEventUUID;

    public TransactionService(TransactionRepository transactionRepository, KafkaSender kafkaSender) {
        TransactionRepository = transactionRepository;
        this.kafkaSender = kafkaSender;
    }

    @Transactional
    public void createTransaction(Transaction transactionDto){
        // eventUUID farkli olana kadar olsuturuyir
        do{
            String newEventUUID = UUID.randomUUID().toString();
        }while( TransactionRepository.existsByEventId(newEventUUID));


        KafkaTransactionTopicMessageDto dto = KafkaTransactionTopicMessageDto.builder()
                .eventUUID(newEventUUID)
                .senderIban(transactionDto.getSenderIban())
                .receiverIban(transactionDto.getReceiverIban())
                .receiverName(transactionDto.getReceiverName())
                .receiverSurname(transactionDto.getReceiverSurname())
                .money(transactionDto.getAmount())
                .description(transactionDto.getDescription())
                .build();


        transaction transactionModel = transaction.builder()
                .eventId(newEventUUID)
                .senderIban(transactionDto.getSenderIban())
                .receiverIban(transactionDto.getReceiverIban())
                .receiverName(transactionDto.getReceiverName())
                .receiverSurname(transactionDto.getReceiverSurname())
                .money(transactionDto.getAmount())
                .description(transactionDto.getDescription())
                .build();

        try{
            TransactionRepository.save(transactionModel);

        }catch (Exception e){
            throw new TransactionSaveException("An Error With Save Transaction " + e.getMessage());
        }

        try{
            kafkaSender.sendTransaction(dto.getEventUUID(),dto);
        }catch (Exception e){
            throw new KafkaSendExceptionOnService("An Error With Send Kafka" + e.getMessage());
        }
    }


    public List<transaction> getTransactionHistory(String userId) {
        try{
            log.info("getTransactionHistory methoduna istek geldi {}" , userId);
            return TransactionRepository.findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(userId, userId);
        }catch(Exception e){
            log.warn("getTransactionHistory Hatasi {} Hata : {}" , userId , e.getMessage());
            throw new TransactionNotFoundException("Transaction Not Found" + e.getMessage());
        }
    }

    public List<transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try{
            log.info("getTransactionsByDateRange methoduna istek geldi startDate {} endDate {}" , startDate,endDate);

            return TransactionRepository.findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(startDate, endDate);
        }catch (Exception e){
            log.warn("getTransactionsByDateRange  StartDate : {} EndDate : {} Exception : {}"  , startDate , endDate, e.getMessage());
            throw new TransactionNotFoundException("Transaction Not Found" + e.getMessage());
        }
    }

    public List<transaction> getErrorLogs() {
        try{
            log.info("getErrorLogs methoduna istek geldi ");
            return TransactionRepository.findByErrorTrue();
        }catch (Exception e){
            log.warn("getErrorLogs sorgusu failed {}", e.getMessage());
            throw new GetErrorLogsException("GetErrorLogs Sorgusu Failed " + e.getMessage());
        }
    }

    public transaction getTransactionById(String id) {
        log.info("getTransactionById methoduna istek geldi {}", id);
        return TransactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
    }
}