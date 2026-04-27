package com.banking_microservices.transaction_service.service;

import com.banking_microservices.transaction_service.dto.TransactionHistory;
import com.banking_microservices.transaction_service.dto.TransactionRequestDto;
import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.dto.enums.SagaStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionType;
import com.banking_microservices.transaction_service.dto.enums.TransferStatus;
import com.banking_microservices.transaction_service.exception.*;
import com.banking_microservices.transaction_service.kafka.KafkaSender;
import com.banking_microservices.transaction_service.model.SagaEvents;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.repository.SagaEventsRepository;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type,
                            ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type,
                            ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();
    private final KafkaSender kafkaSender;
    private final Supplier<String> currentTime;
    private final SagaEventsRepository sagaEventsRepository;

    public TransactionService(TransactionRepository transactionRepository, KafkaSender kafkaSender,
                              Supplier<String> currentTime, SagaEventsRepository sagaEventsRepository) {
        this.transactionRepository = transactionRepository;
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
        this.sagaEventsRepository = sagaEventsRepository;
    }

    @Transactional
    public void saveTransaction(KafkaTransactionTopicMessageDto topicMessage) {
        log.info(" ({}) > TransactionService | saveTransaction -> Metoda veri geldi. Dto:\n{}", currentTime.get(),
                gson.toJson(topicMessage));
        TransactionEntity transactiondata = TransactionEntity.builder()
                .eventId(topicMessage.getEventUUID())
                .senderName(topicMessage.getSenderName())
                .senderSurname(topicMessage.getSenderSurname())
                .senderEmail(topicMessage.getSenderEmail())
                .receiverEmail(topicMessage.getReceiverEmail())
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
                .statusDescription(topicMessage.getStatusDescription())
                .error(topicMessage.getError())
                .errorDescription(topicMessage.getErrorDescription())
                .userValidation(topicMessage.getUserValidation())
                .localDateTime(topicMessage.getLocalDateTime())
                .transferStatus(mapToTransferStatus(topicMessage.getStatus()))
                .build();

        try {
            transactionRepository.save(transactiondata);
            log.info(" ({}) > TransactionService | saveTransaction -> TransactionEntity save succesfully. Dto:\n{}",
                    currentTime.get(), gson.toJson(transactiondata));
        } catch (Exception e) {
            log.warn(" ({}) > TransactionService | saveTransaction -> Hata olustu! Dto:\n{}, Hata: {}",
                    currentTime.get(), gson.toJson(transactiondata), e.getMessage());
            throw new TransactionSaveException(
                    "An Error With Save TransactionEntity. Details : " + e.getMessage() + gson.toJson(transactiondata));
        }
    }

    @Transactional
    public void createTransaction(TransactionRequestDto transactionDto, String senderUserId, String senderMail,
            String senderName, String senderSurname) {
        log.info(" ({}) > TransactionService | createTransaction -> Metoda veri geldi. Dto:\n{}, UserId: {}",
                currentTime.get(), gson.toJson(transactionDto), senderUserId);

        TransactionType txType = transactionDto.getTransactionType() != null
                ? transactionDto.getTransactionType()
                : TransactionType.TRANSFER;

        String newEventUUID;
        do {
            newEventUUID = UUID.randomUUID().toString();
        } while (transactionRepository.existsByEventId(newEventUUID));

        // Determine IBAN values based on transaction type
        String senderIban = null;
        String receiverIban = null;
        String receiverName = null;
        String receiverSurname = null;

        if (txType == TransactionType.DEPOSIT) {
            // Deposit: senderIban is the target account IBAN, receiverIban same
            senderIban = transactionDto.getSenderIban();
            receiverIban = transactionDto.getSenderIban();
        } else if (txType == TransactionType.WITHDRAW) {
            // Withdraw: only senderIban needed
            senderIban = transactionDto.getSenderIban();
        } else {
            // TRANSFER: both IBans and receiver name/surname required
            senderIban = transactionDto.getSenderIban();
            receiverIban = transactionDto.getReceiverIban();
            receiverName = transactionDto.getReceiverName();
            receiverSurname = transactionDto.getReceiverSurname();
        }

        KafkaTransactionTopicMessageDto dto = buildKafkaDto(
                newEventUUID, senderUserId, senderMail, senderName, senderSurname,
                senderIban, receiverIban, receiverName, receiverSurname,
                transactionDto.getAmount(), transactionDto.getDescription(), txType,
                senderUserId);

        TransactionEntity transactionModel = buildTransactionEntity(
                newEventUUID, senderUserId, senderMail, senderName, senderSurname,
                senderIban, receiverIban, receiverName, receiverSurname,
                transactionDto.getAmount(), transactionDto.getDescription(), txType);

        try {
            transactionRepository.save(transactionModel);
            log.info(" ({}) > TransactionService | createTransaction -> Model kaydedildi. Dto:\n{}", currentTime.get(),
                    gson.toJson(transactionModel));
        } catch (Exception e) {
            log.warn(" ({}) > TransactionService | createTransaction -> Model kaydedilemedi! Hata: {}",
                    currentTime.get(), e.getMessage());
            throw new TransactionSaveException("An Error With Save TransactionEntity " + e.getMessage());
        }
        try {
            if (txType == TransactionType.WITHDRAW && dto.getSenderIban() != null) {
                dto.setSenderTransactionHistory(
                        transactionRepository.findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc(
                                dto.getSenderIban(), dto.getSenderIban()));
            } else if (dto.getReceiverIban() != null) {
                dto.setReceiverTransactionHistory(
                        transactionRepository.findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc(
                                dto.getReceiverIban(), dto.getReceiverIban()));
            }
        } catch (Exception e) {
            log.warn(
                    " ({}) > TransactionService | createTransaction -> Transaction history alinamadi, devam ediliyor: {}",
                    currentTime.get(), e.getMessage());
        }
        try {
            log.info(" ({}) > TransactionService | createTransaction -> Kafkaya mesaj atiliyor. Dto:\n{}",
                    currentTime.get(), gson.toJson(dto));
            kafkaSender.sendTransaction(dto.getEventUUID(), dto);
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | createTransaction -> Kafkaya mesaj atilamadi! Hata: {}",
                    currentTime.get(), e.getMessage());
            throw new KafkaSendExceptionOnService("An Error With Send Kafka" + e.getMessage());
        }
    }

    public List<TransactionEntity> getTransactionHistory(String userId) {
        log.info(" ({}) > TransactionService | getTransactionHistory -> methoduna istek geldi {}", currentTime.get(),
                userId);
        try {
            return transactionRepository.findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(userId, userId);
        } catch (Exception e) {
            log.warn(" ({}) > TransactionService | getTransactionHistory -> Hatasi {} Hata : {}", currentTime.get(),
                    userId, e.getMessage());
            throw new TransactionNotFoundException("TransactionEntity Not Found" + e.getMessage());
        }
    }

    public List<TransactionEntity> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info(
                " ({}) > TransactionService | getTransactionsByDateRange -> methoduna istek geldi startDate {} endDate {}",
                currentTime.get(), startDate, endDate);
        try {
            return transactionRepository.findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(startDate, endDate);
        } catch (Exception e) {
            log.warn(
                    " ({}) > TransactionService | getTransactionsByDateRange -> StartDate : {} EndDate : {} Exception : {}",
                    currentTime.get(), startDate, endDate, e.getMessage());
            throw new TransactionNotFoundException("TransactionEntity Not Found" + e.getMessage());
        }
    }

    public List<TransactionEntity> getErrorLogs() {
        log.info(" ({}) > TransactionService | getErrorLogs -> methoduna istek geldi", currentTime.get());
        try {
            return transactionRepository.findByErrorTrue();
        } catch (Exception e) {
            log.warn(" ({}) > TransactionService | getErrorLogs -> sorgusu failed {}", currentTime.get(),
                    e.getMessage());
            throw new GetErrorLogsException("GetErrorLogs Sorgusu Failed " + e.getMessage());
        }
    }

    public TransactionEntity getTransactionById(String id) {
        log.info(" ({}) > TransactionService | getTransactionById -> methoduna istek geldi {}", currentTime.get(), id);
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("TransactionEntity not found with id: " + id));
    }

    @Transactional
    public void updateTransactionStatus(KafkaTransactionTopicMessageDto dto) {
        var entityOpt = transactionRepository.findByEventId(dto.getEventUUID());
        if (entityOpt.isEmpty()) {
            log.warn(" ({}) > TransactionService | updateTransactionStatus -> Entity bulunamadi! EventUUID: {} Status: {}. DB'de kayit yok, guncelleme atlanacak.",
                    currentTime.get(), dto.getEventUUID(), dto.getStatus());
            return;
        }
        var entity = entityOpt.get();
        log.info(" ({}) > TransactionService | updateTransactionStatus -> Onceki status: {} -> Yeni status: {} EventUUID: {}",
                currentTime.get(), entity.getStatus(), dto.getStatus(), dto.getEventUUID());
        entity.setStatus(dto.getStatus());
        entity.setStatusDescription(dto.getStatusDescription());
        entity.setError(dto.getError());
        entity.setErrorDescription(dto.getErrorDescription());
        entity.setTransferStatus(mapToTransferStatus(dto.getStatus()));

        // DÜZELTME: User-service validation sonrası DTO'ya eklenen sender/receiver bilgilerini
        // de entity'ye yansıt. Önceden sadece status güncelleniyordu ve işlem geçmişinde
        // isimler boş kalıyordu.
        if (dto.getSenderName() != null && entity.getSenderName() == null) {
            entity.setSenderName(dto.getSenderName());
        }
        if (dto.getSenderSurname() != null && entity.getSenderSurname() == null) {
            entity.setSenderSurname(dto.getSenderSurname());
        }
        if (dto.getSenderEmail() != null && entity.getSenderEmail() == null) {
            entity.setSenderEmail(dto.getSenderEmail());
        }
        if (dto.getReceiverName() != null && entity.getReceiverName() == null) {
            entity.setReceiverName(dto.getReceiverName());
        }
        if (dto.getReceiverSurname() != null && entity.getReceiverSurname() == null) {
            entity.setReceiverSurname(dto.getReceiverSurname());
        }
        if (dto.getReceiverEmail() != null && entity.getReceiverEmail() == null) {
            entity.setReceiverEmail(dto.getReceiverEmail());
        }
        if (dto.getReceiverUserId() != null && entity.getReceiverUserId() == null) {
            entity.setReceiverUserId(dto.getReceiverUserId());
        }
        if (dto.getUserValidation() != null) {
            entity.setUserValidation(dto.getUserValidation());
        }

        try {
            transactionRepository.save(entity);
            log.info(" ({}) > TransactionService | updateTransactionStatus -> Transaction status GUNCELLENDI: {} -> {}",
                    currentTime.get(), dto.getEventUUID(), dto.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | updateTransactionStatus -> Status guncellenemedi! EventUUID: {}, Hata: {}",
                    currentTime.get(), dto.getEventUUID(), e.getMessage());
            throw new TransactionSaveException("Status guncellenemedi: " + e.getMessage());
        }
    }

    private KafkaTransactionTopicMessageDto buildKafkaDto(
            String eventUUID, String senderUserId, String senderEmail, String senderName, String senderSurname,
            String senderIban, String receiverIban, String receiverName, String receiverSurname,
            BigDecimal money, String description, TransactionType transactionType, String keycloakUUID) {
        return KafkaTransactionTopicMessageDto.builder()
                .eventUUID(eventUUID)
                .senderUserId(senderUserId)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .senderSurname(senderSurname)
                .senderIban(senderIban)
                .receiverIban(receiverIban)
                .receiverName(receiverName)
                .receiverSurname(receiverSurname)
                .money(money)
                .description(description)
                .transactionType(transactionType)
                .keycloakUserUUID(keycloakUUID)
                .status(TransactionStatus.CREATED)
                .statusDescription(TransactionStatus.CREATED.getDescription())
                .build();
    }

    private TransactionEntity buildTransactionEntity(
            String eventId, String senderUserId, String senderEmail, String senderName, String senderSurname,
            String senderIban, String receiverIban, String receiverName, String receiverSurname,
            BigDecimal money, String description, TransactionType transactionType) {
        return TransactionEntity.builder()
                .eventId(eventId)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .senderSurname(senderSurname)
                .senderEmail(senderEmail)
                .senderIban(senderIban)
                .receiverIban(receiverIban)
                .receiverName(receiverName)
                .receiverSurname(receiverSurname)
                .money(money)
                .description(description)
                .transactionType(transactionType)
                .status(TransactionStatus.CREATED)
                .statusDescription(TransactionStatus.CREATED.getDescription())
                .transferStatus(TransferStatus.CREATED)
                .build();
    }

    private TransferStatus mapToTransferStatus(TransactionStatus status) {
        if (status == null) {
            return TransferStatus.CREATED;
        } else if (status == TransactionStatus.CREATED) {
            return TransferStatus.CREATED;
        } else if (status == TransactionStatus.VALIDATION_PENDING || status == TransactionStatus.FRAUD_REVIEW) {
            return TransferStatus.SENT_TO_FRAUD;
        } else if (status == TransactionStatus.BLOCK_MONEY) {
            return TransferStatus.SENT_TO_MONEY;
        } else if (status == TransactionStatus.COMPLETED) {
            return TransferStatus.COMPLETED;
        } else if (status == TransactionStatus.BLOCK_MONEY_FAILED || status == TransactionStatus.DEPOSIT_FAILED
                || status == TransactionStatus.WITHDRAW_FAILED || status == TransactionStatus.FAILED) {
            return TransferStatus.FAILED;
        } else {
            return TransferStatus.CREATED;
        }
    }


    public void createSagaEvent(String eventUUID) {
        log.info(" ({}) > TransactionService | createSagaEvent -> Metoda veri geldi. EventUUID: {}", currentTime.get(), eventUUID);

        if (eventUUID == null || eventUUID.isBlank()) {
            log.warn(" ({}) > TransactionService | createSagaEvent -> EventUUID null veya bos!", currentTime.get());
            throw new TransactionNotFoundException("EventUUID bos olamaz.");
        }

        TransactionEntity transaction = transactionRepository.findByEventId(eventUUID).orElseThrow(() -> {
            log.warn(" ({}) > TransactionService | createSagaEvent -> Transaction bulunamadi! EventUUID: {}", currentTime.get(), eventUUID);
            return new TransactionNotFoundException("Transaction not found : " + eventUUID);
        });

        log.info(" ({}) > TransactionService | createSagaEvent -> Transaction bulundu. EventUUID: {}, Status: {}", currentTime.get(), eventUUID, transaction.getStatus());

        SagaEvents sagaEvent = SagaEvents.builder()
                .kafkaEventUUID(eventUUID)
                .status(SagaStatus.CREATED)
                .transactionHistory(
                        TransactionHistory.builder()
                                .eventId(transaction.getEventId())
                                .description(transaction.getDescription())
                                .money(transaction.getMoney())
                                .senderUserId(transaction.getSenderUserId())
                                .receiverUserId(transaction.getReceiverUserId())
                                .build()
                )
                .build();

        try {
            kafkaSender.sendSagaEvent(sagaEvent);
            log.info(" ({}) > TransactionService | createSagaEvent -> Saga event Kafkaya gonderildi. UUID: {}", currentTime.get(), sagaEvent.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | createSagaEvent -> Saga event Kafkaya gonderilemedi! UUID: {}, Hata: {}", currentTime.get(), sagaEvent.getUUID(), e.getMessage());
            throw new SagaEventDatabaseSaveException("Saga event Kafka gonderimi basarisiz: " + e.getMessage());
        }

        try {
            sagaEventsRepository.save(sagaEvent);
            log.info(" ({}) > TransactionService | createSagaEvent -> Saga event veritabanina kaydedildi. UUID: {}", currentTime.get(), sagaEvent.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | createSagaEvent -> Saga event veritabanina kaydedilemedi! UUID: {}, Hata: {}", currentTime.get(), sagaEvent.getUUID(), e.getMessage());
            throw new SagaEventDatabaseSaveException("An Exception With Save Database saga Event Model. " + e.getMessage());
        }

    }

    @Transactional
    public void updateSagaEventStatus(SagaEvents sagaEvents) {

        if (sagaEvents == null || sagaEvents.getUUID() == null) {
            log.warn(" ({}) > TransactionService | updateSagaEventStatus -> sagaEvents veya UUID null, isleme atlanacak.", currentTime.get());
            return;
        }

        log.info(" ({}) > TransactionService | updateSagaEventStatus -> Metoda veri geldi. UUID: {}, Status: {}", currentTime.get(), sagaEvents.getUUID(), sagaEvents.getStatus());

        SagaEvents existingSagaEvent = sagaEventsRepository.findById(sagaEvents.getUUID()).orElseThrow(() -> {
            log.warn(" ({}) > TransactionService | updateSagaEventStatus -> Saga event bulunamadi! UUID: {}", currentTime.get(), sagaEvents.getUUID());
            return new SagaEventNotFoundException("Saga event not found : " + sagaEvents.getUUID());
        });

        log.info(" ({}) > TransactionService | updateSagaEventStatus -> Mevcut status: {} -> Yeni status: {} UUID: {}", currentTime.get(), existingSagaEvent.getStatus(), sagaEvents.getStatus(), sagaEvents.getUUID());

        existingSagaEvent.setStatus(sagaEvents.getStatus());
        existingSagaEvent.setErrorDescripton(sagaEvents.getErrorDescripton());

        if (sagaEvents.getTransactionHistory() != null) {
            existingSagaEvent.setTransactionHistory(sagaEvents.getTransactionHistory());
        }

        try {
            sagaEventsRepository.save(existingSagaEvent);
            log.info(" ({}) > TransactionService | updateSagaEventStatus -> Saga event status GUNCELLENDI. UUID: {}, Status: {}", currentTime.get(), existingSagaEvent.getUUID(), existingSagaEvent.getStatus());
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | updateSagaEventStatus -> Saga event status guncellenemedi! UUID: {}, Hata: {}", currentTime.get(), existingSagaEvent.getUUID(), e.getMessage());
            throw new SagaEventDatabaseSaveException("Saga event status guncellenemedi: " + e.getMessage());
        }
    }

    


}