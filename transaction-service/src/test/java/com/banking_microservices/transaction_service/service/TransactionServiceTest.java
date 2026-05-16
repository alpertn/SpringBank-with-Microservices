package com.banking_microservices.transaction_service.service;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.dto.TokenDetailsDto;
import com.banking_microservices.transaction_service.dto.TransactionRequestDto;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionType;
import com.banking_microservices.transaction_service.dto.enums.TransferStatus;
import com.banking_microservices.transaction_service.kafka.KafkaSender;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.repository.SagaEventsRepository;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaSender kafkaSender;

    @Mock
    private SagaEventsRepository sagaEventsRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, kafkaSender, () -> "12:00:00", sagaEventsRepository);
    }

    @Test
    void createTransactionPersistsCreatedTransferAndPublishesKafkaMessage() {
        TransactionRequestDto request = TransactionRequestDto.builder()
                .amount(new BigDecimal("1000.00"))
                .transactionType(TransactionType.TRANSFER)
                .senderIban("TRSENDER")
                .receiverIban("TRRECEIVER")
                .receiverName("Receiver")
                .receiverSurname("User")
                .description("integration transfer")
                .build();
        TokenDetailsDto tokenDetails = TokenDetailsDto.builder()
                .subject("sender-keycloak")
                .email("sender@springbank.test")
                .build();
        when(transactionRepository.existsByEventId(any())).thenReturn(false);
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc("TRRECEIVER", "TRRECEIVER"))
                .thenReturn(List.of());

        transactionService.createTransaction(request, "sender-keycloak", "sender@springbank.test", "Sender", "User", tokenDetails);

        ArgumentCaptor<TransactionEntity> entityCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(entityCaptor.capture());
        TransactionEntity entity = entityCaptor.getValue();
        assertThat(entity.getMoney()).isEqualByComparingTo("1000.00");
        assertThat(entity.getStatus()).isEqualTo(TransactionStatus.CREATED);
        assertThat(entity.getTransferStatus()).isEqualTo(TransferStatus.CREATED);
        assertThat(entity.getSenderIban()).isEqualTo("TRSENDER");
        assertThat(entity.getReceiverIban()).isEqualTo("TRRECEIVER");

        ArgumentCaptor<KafkaTransactionTopicMessageDto> kafkaCaptor = ArgumentCaptor.forClass(KafkaTransactionTopicMessageDto.class);
        verify(kafkaSender).sendTransaction(any(), kafkaCaptor.capture());
        assertThat(kafkaCaptor.getValue().getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(kafkaCaptor.getValue().getTokenDetails()).isSameAs(tokenDetails);
    }

    @Test
    void updateTransactionStatusMapsCompletedResultToCompletedTransferStatus() {
        TransactionEntity entity = TransactionEntity.builder()
                .eventId("event-1")
                .status(TransactionStatus.CREATED)
                .transferStatus(TransferStatus.CREATED)
                .build();
        when(transactionRepository.findByEventId("event-1")).thenReturn(Optional.of(entity));

        transactionService.updateTransactionStatus(KafkaTransactionTopicMessageDto.builder()
                .eventUUID("event-1")
                .status(TransactionStatus.COMPLETED)
                .statusDescription("completed")
                .error(false)
                .build());

        assertThat(entity.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(entity.getTransferStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(transactionRepository).save(entity);
    }

    @Test
    void getTransactionHistoryReadsSenderOrReceiverHistory() {
        when(transactionRepository.findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc("user-1", "user-1"))
                .thenReturn(List.of(TransactionEntity.builder().senderUserId("user-1").build()));

        assertThat(transactionService.getTransactionHistory("user-1")).hasSize(1);
    }
}
