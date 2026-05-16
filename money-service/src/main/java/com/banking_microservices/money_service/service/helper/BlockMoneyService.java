package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.exception.DecramentAndBlockMoneyException;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockMoneyService {

    private final UserMoneyRepository repository;
    private final KafkaSender kafkaSender;
    private final TransactionErrorHandler errorHandler;
    private final Supplier<String> currentTime;

    public void blockFunds(KafkaTransactionTopicMessageDto dto) {
        // sadece DB islemi try icerisinde. Kafka gonderimi disarida.
        try {
            int updatedRowCount = repository.decrementAndBlockByIban(dto.getSenderIban(), dto.getMoney());
            if (updatedRowCount == 0) {
                errorHandler.sendErrorAndThrow(
                        dto,
                        "Sender IBAN not found or insufficient balance for block operation.",
                        new DecramentAndBlockMoneyException(
                                "Block money failed for iban=" + dto.getSenderIban()
                        )
                );
            }
        } catch (DecramentAndBlockMoneyException exception) {
            throw exception;
        } catch (Exception e) {
            log.error(" ({}) > BlockMoneyService | blockFunds -> Para bloke edilirken hata olustu! Hata: {}", currentTime.get(), e.getMessage());
            errorHandler.sendErrorAndThrow(dto, "An Exception with decrement money and block money with iban.", new DecramentAndBlockMoneyException("An exception with Decrement Money And Block money with Iban number. Iban = " + dto.getSenderIban()));
        }

        // DB basarili. DTO guncelle ve Kafkaya bildir.
        dto.setIsMoneyBlocked(true);
        dto.setStatus(TransactionStatus.BLOCK_MONEY);
        dto.setStatusDescription(TransactionStatus.BLOCK_MONEY.getDescription());

        log.info(" ({}) > BlockMoneyService | blockFunds -> Para bloke edildi ve Kafkaya gonderiliyor. {}", currentTime.get(), dto);

        kafkaSender.sendBlockedMoneyTopic(dto.getEventUUID(), dto);
    }
}
