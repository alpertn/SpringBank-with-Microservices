package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.kafka.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bu class {@link KafkaSender} classini cagirir.
 *
 * Hata senaryolarinda Kafkaya error gonderip exception firlatmayi tek noktada toplar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionErrorHandler {

    private final KafkaSender kafkaSender;

    /**
     * DTO'ya hata bilgisini set eder, Kafkaya error gonderir ve exception firlatir.
     *
     * @param dto islem DTOsu
     * @param description hata aciklamasi
     * @param exception firlatilacak exception
     */
    public <T extends RuntimeException> void sendErrorAndThrow(KafkaTransactionTopicMessageDto dto, String description, T exception) {
        dto.setError(true);
        dto.setErrorDescription(description);
        kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
        log.error(" > TransactionErrorHandler | sendErrorAndThrow -> Hata Kafkaya iletildi. EventUUID: {}, Aciklama: {}", dto.getEventUUID(), description);
        throw exception;
    }
}
