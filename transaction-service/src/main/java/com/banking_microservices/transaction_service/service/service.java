package com.banking_microservices.transaction_service.service;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.*;
import com.banking_microservices.transaction_service.model.transaction;
import com.banking_microservices.transaction_service.repository.repository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class service {

    private final repository repository;
    private final Gson gson = new Gson();

    public void createNewTransaction(Transaction transactionData){
        log.info("veri transaction servisine geldi {}", transactionData);
        try{
            transaction transactionModel = transaction
                    .builder()
                    .senderIban(transactionData.getSenderIban())
                    .receiverIban(transactionData.getReceiverIban())
                    .money(transactionData.getAmount())
                    .transactionType("TRANSFER")
                    .build();

            repository.save(transactionModel);

            try{

            }catch (Exception e){
                throw new KafkaSendException("Kafka Exception. " +  gson.toJson(transactionData));
            }

            }catch (Exception e){
            throw new TransactionDtoSyntaxException("Bir hata meydana geldi " + gson.toJson(transactionData));
        }
    }





    public transaction createTransaction(KafkaTransactionTopicMessageDto request) {
        log.info("Transaction Kaydedilme istegi geldi. {}", gson.toJson(request));

        try {
            transaction newTransaction = transaction.builder()
                    .transactionType(request.getTransactionType())
                    .senderUserId(request.getSenderUserId())
                    .receiverUserId(request.getReceiverUserId())
                    .senderIban(request.getSenderIban())
                    .receiverIban(request.getReceiverIban())
                    .money(request.getMoney())
                    .description(request.getDescription())
                    .error(request.getError())
                    .errorDescription(request.getErrorDescription())
                    .status(request.getStatus())
                    .build();

            transaction savedTransaction = repository.save(newTransaction);
            log.info("Transaction Saved {}", gson.toJson(savedTransaction));
            return savedTransaction;

        } catch (Exception e) {
            log.warn("Transaction Kaydedilirken Sorun olustu. {}", e.getMessage());
            throw new TransactionSaveException("Transaction Kaydedilirken Hata olustu Bilgiler : " + gson.toJson(request));
        }
    }

    public List<transaction> getTransactionHistory(String userId) {
        try{
            log.info("getTransactionHistory methoduna istek geldi {}" , userId);
            return repository.findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(userId, userId);
        }catch(Exception e){
            log.warn("getTransactionHistory Hatasi {} Hata : {}" , userId , e.getMessage());
            throw new TransactionNotFoundException("Transaction Not Found" + e.getMessage());
        }
    }

    public List<transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try{
            log.info("getTransactionsByDateRange methoduna istek geldi startDate {} endDate {}" , startDate,endDate);

            return repository.findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(startDate, endDate);
        }catch (Exception e){
            log.warn("getTransactionsByDateRange  StartDate : {} EndDate : {} Exception : {}"  , startDate , endDate, e.getMessage());
            throw new TransactionNotFoundException("Transaction Not Found" + e.getMessage());
        }
    }

    public List<transaction> getErrorLogs() {
        try{
            log.info("getErrorLogs methoduna istek geldi ");
            return repository.findByErrorTrue();
        }catch (Exception e){
            log.warn("getErrorLogs sorgusu failed {}", e.getMessage());
            throw new GetErrorLogsException("GetErrorLogs Sorgusu Failed " + e.getMessage());
        }
    }

    public transaction getTransactionById(String id) {
        log.info("getTransactionById methoduna istek geldi {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
    }
}