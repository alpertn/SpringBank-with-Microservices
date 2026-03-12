package com.banking_microservices.transaction_service.service;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.*;
import com.banking_microservices.transaction_service.kafka.KafkaSender;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final KafkaSender kafkaSender;

    public TransactionService(TransactionRepository transactionRepository, KafkaSender kafkaSender) {
        this.transactionRepository = transactionRepository;
        this.kafkaSender = kafkaSender;
    }

    @Transactional
    public void saveTransaction(KafkaTransactionTopicMessageDto topicMessage) {

        TransactionEntity transactiondata = TransactionEntity.builder()
                .eventId(topicMessage.getEventUUID())
                .receiverName(topicMessage.getReceiverName())
                .receiverSurname(topicMessage.getReceiverSurname())
                .senderUserId(topicMessage.getSenderUserId())
                .receiverUserId(topicMessage.getReceiverUserId())
                .senderIban(topicMessage.getSenderIban())
                .receiverIban(topicMessage.getReceiverIban())
                .money(topicMessage.getMoney())
                .transactionType(topicMessage.getTransactionType())
                .description(topicMessage.getDescription())
                .status(topicMessage.getStatus())
                .error(topicMessage.getError())
                .errorDescription(topicMessage.getErrorDescription())
                .localDateTime(topicMessage.getLocalDateTime())
                .build();

        try {
            transactionRepository.save(transactiondata);
            log.info("TransactionEntity save succesfully {}", gson.toJson(transactiondata));
        } catch (Exception e) {
            throw new TransactionSaveException(
                    "An Error With Save TransactionEntity. Details : " + e.getMessage() + gson.toJson(transactiondata));
        }

    }

    @Transactional
    public void createTransaction(Transaction transactionDto, String senderUserId, String senderMail, String senderName, String senderSurname) {
        String newEventUUID;
        do {
            newEventUUID = UUID.randomUUID().toString();
        } while (transactionRepository.existsByEventId(newEventUUID));


        KafkaTransactionTopicMessageDto dto = KafkaTransactionTopicMessageDto.builder()
                .eventUUID(newEventUUID)
                .senderUserId(senderUserId)
                .senderEmail(senderMail)
                .senderName(senderName)
                .senderSurname(senderSurname)
                .receiverIban(transactionDto.getReceiverIban())
                .receiverName(transactionDto.getReceiverName())
                .receiverSurname(transactionDto.getReceiverSurname())
                .money(transactionDto.getAmount())
                .description(transactionDto.getDescription())
                .build();

        TransactionEntity transactionModel = TransactionEntity.builder()
                .eventId(newEventUUID)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .senderSurname(senderSurname)
                .senderEmail(senderMail)
                .receiverIban(transactionDto.getReceiverIban())
                .receiverName(transactionDto.getReceiverName())
                .receiverSurname(transactionDto.getReceiverSurname())
                .money(transactionDto.getAmount())
                .description(transactionDto.getDescription())
                .build();

        try {
            transactionRepository.save(transactionModel);
        } catch (Exception e) {
            throw new TransactionSaveException("An Error With Save TransactionEntity " + e.getMessage());
        }
        try{
            dto.setSenderTransactionHistory(transactionRepository.findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc(dto.getSenderIban(), dto.getSenderIban()));
            dto.setReceiverTransactionHistory(transactionRepository.findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc(dto.getReceiverIban(),dto.getReceiverIban()));

        }catch (Exception e){
            throw new GetEventHistoryException("An Exception With get Event History for transaction " + dto.getSenderIban() + ' ' + dto.getReceiverIban());
        }
        try {
            kafkaSender.sendTransaction(dto.getEventUUID(), dto);
        } catch (Exception e) {
            throw new KafkaSendExceptionOnService("An Error With Send Kafka" + e.getMessage());
        }
    }

    public List<TransactionEntity> getTransactionHistory(String userId) {
        try {
            log.info("getTransactionHistory methoduna istek geldi {}", userId);
            return transactionRepository.findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(userId, userId);
        } catch (Exception e) {
            log.warn("getTransactionHistory Hatasi {} Hata : {}", userId, e.getMessage());
            throw new TransactionNotFoundException("TransactionEntity Not Found" + e.getMessage());
        }
    }

    public List<TransactionEntity> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            log.info("getTransactionsByDateRange methoduna istek geldi startDate {} endDate {}", startDate, endDate);

            return transactionRepository.findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(startDate, endDate);
        } catch (Exception e) {
            log.warn("getTransactionsByDateRange  StartDate : {} EndDate : {} Exception : {}", startDate, endDate,
                    e.getMessage());
            throw new TransactionNotFoundException("TransactionEntity Not Found" + e.getMessage());
        }
    }

    public List<TransactionEntity> getErrorLogs() {
        try {
            log.info("getErrorLogs methoduna istek geldi ");
            return transactionRepository.findByErrorTrue();
        } catch (Exception e) {
            log.warn("getErrorLogs sorgusu failed {}", e.getMessage());
            throw new GetErrorLogsException("GetErrorLogs Sorgusu Failed " + e.getMessage());
        }
    }

    public TransactionEntity getTransactionById(String id) {
        log.info("getTransactionById methoduna istek geldi {}", id);
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("TransactionEntity not found with id: " + id));
    }
}