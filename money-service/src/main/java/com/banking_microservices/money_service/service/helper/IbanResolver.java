package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.IbanNotFoundException;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IbanResolver {

    private final UserMoneyRepository repository;
    private final TransactionErrorHandler errorHandler;
    private final Supplier<String> currentTime;

    public void resolveSenderIban(KafkaTransactionTopicMessageDto dto) {
        if (isMissing(dto.getSenderIban()) && dto.getSenderUserId() != null) {
            String resolvedIban = repository.findIbanByUserId(dto.getSenderUserId()).orElse(null);
            dto.setSenderIban(resolvedIban);
            log.info(" ({}) > IbanResolver | resolveSenderIban -> SenderIban userId uzerinden resolve edildi. UserId: {}, Iban: {}", currentTime.get(), dto.getSenderUserId(), resolvedIban);
        }
    }

    public void assertSenderIbanExists(KafkaTransactionTopicMessageDto dto) {
        if (isMissing(dto.getSenderIban())) {
            log.warn(" ({}) > IbanResolver | assertSenderIbanExists -> Sender IBAN bulunamadi! {}", currentTime.get(), dto);
            errorHandler.sendErrorAndThrow(dto, "Sender Iban Not Found", new IbanNotFoundException("Iban value is empty"));
        }
    }

    public String resolveReceiverUserIdOrThrow(KafkaTransactionTopicMessageDto dto) {
        String receiverUserId = repository.findUserIdByIban(dto.getReceiverIban()).orElse(null);
        if (receiverUserId == null) {
            log.warn(" ({}) > IbanResolver | resolveReceiverUserIdOrThrow -> Receiver IBAN bulunamadi! IBAN: {}", currentTime.get(), dto.getReceiverIban());
            errorHandler.sendErrorAndThrow(dto, "Receiver Iban Not Found: " + dto.getReceiverIban(), new IbanNotFoundException("Receiver IBAN bulunamadi: " + dto.getReceiverIban()));
        }
        return receiverUserId;
    }

    public BigDecimal getBalanceOrThrow(String iban, String role, KafkaTransactionTopicMessageDto dto) {
        BigDecimal balance = repository.findBalanceByIban(iban).orElse(null);
        if (balance == null) {
            log.warn(" ({}) > IbanResolver | getBalanceOrThrow -> {} hesabi bulunamadi! IBAN: {}", currentTime.get(), role, iban);
            errorHandler.sendErrorAndThrow(dto, role + " Iban Not Found: " + iban, new IbanNotFoundException(role + " hesabi bulunamadi: " + iban));
        }
        return balance;
    }

    public void assertAccountExists(String iban, String role, KafkaTransactionTopicMessageDto dto) {
        getBalanceOrThrow(iban, role, dto);
    }

    private boolean isMissing(String iban) {
        return iban == null || iban.isEmpty();
    }
}
