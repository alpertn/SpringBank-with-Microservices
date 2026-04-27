package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.SagaEventsDto;
import com.banking_microservices.money_service.dto.TransactionEntity;
import com.banking_microservices.money_service.dto.enums.SagaStatus;
import com.banking_microservices.money_service.exception.SagaEventNotFoundException;
import com.banking_microservices.money_service.exception.SagaTransactionRollbackException;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.repository.SagaEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Slf4j
@Service
public class SagaService {

    private final SagaEventsRepository sagaEventsRepository;
    private final UserMoneyService userMoneyService;
    private final KafkaSender kafkaSender;
    private final Supplier<String> currentTime;

    public SagaService(SagaEventsRepository sagaEventsRepository,
                       UserMoneyService userMoneyService,
                       KafkaSender kafkaSender,
                       Supplier<String> currentTime) {
        this.sagaEventsRepository = sagaEventsRepository;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
    }

    @Transactional
    public void handleSagaEvent(SagaEventsDto dto) {
        log.info(" ({}) > SagaService | handleSagaEvent -> Metoda veri geldi. UUID: {}, KafkaEventUUID: {}", currentTime.get(), dto.getUUID(), dto.getKafkaEventUUID());

        if (dto.getKafkaEventUUID() == null) {
            log.warn(" ({}) > SagaService | handleSagaEvent -> KafkaEventUUID null, islem atlanacak.", currentTime.get());
            return;
        }

        // Idempotency: Ayni kafkaEventUUID daha once islendiyse atla
        if (sagaEventsRepository.existsByKafkaEventUUID(dto.getKafkaEventUUID())) {
            log.warn(" ({}) > SagaService | handleSagaEvent -> Bu Saga event zaten islendi, atlaniyor. KafkaEventUUID: {}", currentTime.get(), dto.getKafkaEventUUID());
            return;
        }

        // DB'ye PROCESS statusuyla kaydet
        dto.setStatus(SagaStatus.PROCESS);
        try {
            sagaEventsRepository.save(dto);
            log.info(" ({}) > SagaService | handleSagaEvent -> Saga event PROCESS statusuyla kaydedildi. UUID: {}", currentTime.get(), dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > SagaService | handleSagaEvent -> Saga event kaydedilemedi! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            throw new SagaTransactionRollbackException("Saga event DB kayit hatasi: " + e.getMessage());
        }

        TransactionEntity transactionEntity = dto.getTransactionEntity();
        if (transactionEntity == null) {
            log.warn(" ({}) > SagaService | handleSagaEvent -> TransactionEntity null, saga tamamlanamaz. UUID: {}", currentTime.get(), dto.getUUID());
            sendSagaError(dto, "TransactionEntity null, saga tamamlanamaz.");
            return;
        }

        String transactionType = transactionEntity.getTransactionType();
        log.info(" ({}) > SagaService | handleSagaEvent -> TransactionType: {}, UUID: {}", currentTime.get(), transactionType, dto.getUUID());

        try {
            if ("TRANSFER".equals(transactionType)) {
                handleTransferRollback(dto, transactionEntity);
            } else if ("DEPOSIT".equals(transactionType)) {
                handleDepositRollback(dto, transactionEntity);
            } else if ("WITHDRAW".equals(transactionType)) {
                handleWithdrawRollback(dto, transactionEntity);
            } else {
                log.warn(" ({}) > SagaService | handleSagaEvent -> Bilinmeyen TransactionType: {}, UUID: {}", currentTime.get(), transactionType, dto.getUUID());
                sendSagaError(dto, "Bilinmeyen TransactionType: " + transactionType);
                return;
            }

            // Basarili: COMPLETED olarak guncelle ve saga sender'a gonder
            dto.setStatus(SagaStatus.COMPLETED);
            sagaEventsRepository.save(dto);
            log.info(" ({}) > SagaService | handleSagaEvent -> Saga COMPLETED. UUID: {}", currentTime.get(), dto.getUUID());

            kafkaSender.sendSagaSuccess(dto);
            log.info(" ({}) > SagaService | handleSagaEvent -> Saga success Kafkaya gonderildi. UUID: {}", currentTime.get(), dto.getUUID());

        } catch (SagaTransactionRollbackException e) {
            log.error(" ({}) > SagaService | handleSagaEvent -> Saga rollback basarisiz! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            sendSagaError(dto, e.getMessage());
        } catch (Exception e) {
            log.error(" ({}) > SagaService | handleSagaEvent -> Beklenmeyen hata! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            sendSagaError(dto, "Beklenmeyen hata: " + e.getMessage());
        }
    }

    // ─── TRANSFER: Saga geri alma ─────────────────────────────────────────────
    // Transfer edilmis para tam tersine doner:
    // - SenderIban'a para iade edilir (deposit)
    // - ReceiverIban'dan para geri alinir (withdraw)

    private void handleTransferRollback(SagaEventsDto dto, TransactionEntity tx) {
        log.info(" ({}) > SagaService | handleTransferRollback -> TRANSFER saga rollback baslatiliyor. SenderIban: {}, ReceiverIban: {}, Miktar: {}", currentTime.get(), tx.getSenderIban(), tx.getReceiverIban(), tx.getMoney());

        if (tx.getMoney() == null) {
            throw new SagaTransactionRollbackException("Transfer rollback icin Money null. KafkaEventUUID: " + dto.getKafkaEventUUID());
        }

        if (tx.getSenderIban() == null && tx.getSenderUserId() == null) {
            throw new SagaTransactionRollbackException("Transfer rollback icin SenderIban ve SenderUserId null. KafkaEventUUID: " + dto.getKafkaEventUUID());
        }

        if (tx.getReceiverIban() == null && tx.getReceiverUserId() == null) {
            throw new SagaTransactionRollbackException("Transfer rollback icin ReceiverIban ve ReceiverUserId null. KafkaEventUUID: " + dto.getKafkaEventUUID());
        }

        // ReceiverIban'dan parayı geri al (withdraw)
        try {
            if (tx.getReceiverIban() != null) {
                userMoneyService.withdrawMoneyByIban(tx.getReceiverIban(), tx.getMoney());
                log.info(" ({}) > SagaService | handleTransferRollback -> ReceiverIban'dan para geri alindi. IBAN: {}", currentTime.get(), tx.getReceiverIban());
            } else {
                userMoneyService.withdrawMoneyByUserId(tx.getReceiverUserId(), tx.getMoney());
                log.info(" ({}) > SagaService | handleTransferRollback -> ReceiverUserId'den para geri alindi. UserId: {}", currentTime.get(), tx.getReceiverUserId());
            }
        } catch (Exception e) {
            throw new SagaTransactionRollbackException("ReceiverIban para geri alma basarisiz: " + e.getMessage());
        }

        // SenderIban'a parayı iade et (deposit)
        try {
            if (tx.getSenderIban() != null) {
                userMoneyService.depositMoneyByIban(tx.getSenderIban(), tx.getMoney());
                log.info(" ({}) > SagaService | handleTransferRollback -> SenderIban'a para iade edildi. IBAN: {}", currentTime.get(), tx.getSenderIban());
            } else {
                userMoneyService.depositMoneyByUserId(tx.getSenderUserId(), tx.getMoney());
                log.info(" ({}) > SagaService | handleTransferRollback -> SenderUserId'ye para iade edildi. UserId: {}", currentTime.get(), tx.getSenderUserId());
            }
        } catch (Exception e) {
            throw new SagaTransactionRollbackException("SenderIban para iade basarisiz: " + e.getMessage());
        }

        log.info(" ({}) > SagaService | handleTransferRollback -> TRANSFER rollback tamamlandi.", currentTime.get());
    }

    // ─── DEPOSIT: Saga geri alma ──────────────────────────────────────────────
    // Yatirilan para geri cekiliyor (withdraw)

    private void handleDepositRollback(SagaEventsDto dto, TransactionEntity tx) {
        log.info(" ({}) > SagaService | handleDepositRollback -> DEPOSIT saga rollback baslatiliyor. Miktar: {}", currentTime.get(), tx.getMoney());

        if (tx.getMoney() == null) {
            throw new SagaTransactionRollbackException("Deposit rollback icin Money null. KafkaEventUUID: " + dto.getKafkaEventUUID());
        }

        String targetIban = tx.getReceiverIban() != null ? tx.getReceiverIban() : tx.getSenderIban();

        try {
            if (targetIban != null) {
                userMoneyService.withdrawMoneyByIban(targetIban, tx.getMoney());
                log.info(" ({}) > SagaService | handleDepositRollback -> Deposit rollback (withdraw) tamamlandi. IBAN: {}", currentTime.get(), targetIban);
            } else if (tx.getSenderUserId() != null) {
                userMoneyService.withdrawMoneyByUserId(tx.getSenderUserId(), tx.getMoney());
                log.info(" ({}) > SagaService | handleDepositRollback -> Deposit rollback (withdraw by userId) tamamlandi. UserId: {}", currentTime.get(), tx.getSenderUserId());
            } else {
                throw new SagaTransactionRollbackException("Deposit rollback icin IBAN ve UserId null.");
            }
        } catch (SagaTransactionRollbackException e) {
            throw e;
        } catch (Exception e) {
            throw new SagaTransactionRollbackException("Deposit rollback basarisiz: " + e.getMessage());
        }
    }

    // ─── WITHDRAW: Saga geri alma ─────────────────────────────────────────────
    // Cekilen para geri yatiriliyor (deposit)

    private void handleWithdrawRollback(SagaEventsDto dto, TransactionEntity tx) {
        log.info(" ({}) > SagaService | handleWithdrawRollback -> WITHDRAW saga rollback baslatiliyor. Miktar: {}", currentTime.get(), tx.getMoney());

        if (tx.getMoney() == null) {
            throw new SagaTransactionRollbackException("Withdraw rollback icin Money null. KafkaEventUUID: " + dto.getKafkaEventUUID());
        }

        String targetIban = tx.getSenderIban() != null ? tx.getSenderIban() : tx.getReceiverIban();

        try {
            if (targetIban != null) {
                userMoneyService.depositMoneyByIban(targetIban, tx.getMoney());
                log.info(" ({}) > SagaService | handleWithdrawRollback -> Withdraw rollback (deposit) tamamlandi. IBAN: {}", currentTime.get(), targetIban);
            } else if (tx.getSenderUserId() != null) {
                userMoneyService.depositMoneyByUserId(tx.getSenderUserId(), tx.getMoney());
                log.info(" ({}) > SagaService | handleWithdrawRollback -> Withdraw rollback (deposit by userId) tamamlandi. UserId: {}", currentTime.get(), tx.getSenderUserId());
            } else {
                throw new SagaTransactionRollbackException("Withdraw rollback icin IBAN ve UserId null.");
            }
        } catch (SagaTransactionRollbackException e) {
            throw e;
        } catch (Exception e) {
            throw new SagaTransactionRollbackException("Withdraw rollback basarisiz: " + e.getMessage());
        }
    }

    // ─── Error Helper ─────────────────────────────────────────────────────────

    private void sendSagaError(SagaEventsDto dto, String reason) {
        dto.setStatus(SagaStatus.ERROR);
        dto.setErrorDescripton(reason);

        try {
            sagaEventsRepository.save(dto);
            log.info(" ({}) > SagaService | sendSagaError -> Saga ERROR statusuyla kaydedildi. UUID: {}", currentTime.get(), dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > SagaService | sendSagaError -> Saga ERROR DB kaydi basarisiz! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
        }

        try {
            kafkaSender.sendSagaError(dto);
            log.info(" ({}) > SagaService | sendSagaError -> Saga error Kafkaya gonderildi. UUID: {}", currentTime.get(), dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > SagaService | sendSagaError -> Saga error Kafkaya gonderilemedi! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
        }
    }

}
