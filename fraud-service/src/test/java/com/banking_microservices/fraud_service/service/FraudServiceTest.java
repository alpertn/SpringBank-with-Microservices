package com.banking_microservices.fraud_service.service;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.dto.enums.TransactionStatus;
import com.banking_microservices.fraud_service.kafka.KafkaSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private KafkaSenderService kafkaSenderService;

    private final java.util.function.Supplier<String> currentTime = () -> "12:00:00";

    @Test
    void sendForwardsValidatedTransactionToFraudCheckedTopic() {
        FraudService fraudService = new FraudService(kafkaSenderService, currentTime);
        KafkaTransactionTopicMessageDto dto = KafkaTransactionTopicMessageDto.builder()
                .eventUUID("event-1")
                .status(TransactionStatus.VALIDATION_PENDING)
                .build();

        fraudService.send(dto);

        verify(kafkaSenderService).sendTransaction("event-1", dto);
    }
}
