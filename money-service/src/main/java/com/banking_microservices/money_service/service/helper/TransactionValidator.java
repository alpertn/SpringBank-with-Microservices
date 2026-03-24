package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.MoneyNotAvaibleException;
import com.banking_microservices.money_service.exception.NegativeNumberException;
import com.banking_microservices.money_service.exception.SameAccountException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionValidator {

    private final Supplier<String> currentTime;

    public void assertAmountIsPositive(KafkaTransactionTopicMessageDto dto) {
        if (dto.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            log.error(" ({}) > TransactionValidator | assertAmountIsPositive -> Transfer miktari pozitif olmalidir! Amount: {}", currentTime.get(), dto.getMoney());
            throw new NegativeNumberException("Transfer amount must be positive");
        }
    }

    public void assertNotSameAccount(KafkaTransactionTopicMessageDto dto) {
        if (dto.getReceiverIban().equals(dto.getSenderIban())) {
            log.error(" ({}) > TransactionValidator | assertNotSameAccount -> Gonderen ve Alici Iban ayni olamaz! {}", currentTime.get(), dto.getSenderIban());
            throw new SameAccountException("Cannot transfer to the same account");
        }
    }

    public void assertSufficientBalance(BigDecimal balance, KafkaTransactionTopicMessageDto dto) {
        if (balance.compareTo(dto.getMoney()) <= 0) {
            log.warn(" ({}) > TransactionValidator | assertSufficientBalance -> Bakiye yetersiz! Bankadaki miktar: {} | Istenilen miktar: {}", currentTime.get(), balance, dto.getMoney());
            throw new MoneyNotAvaibleException("Money not avaible KafkaTransactionTopicService");
        }
    }
}
