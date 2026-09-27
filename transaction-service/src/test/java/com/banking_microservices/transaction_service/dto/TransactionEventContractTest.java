package com.banking_microservices.transaction_service.dto;

import com.banking_microservices.transaction_service.model.workflow.TransactionWorkflowState;
import com.banking_microservices.transaction_service.kafka.TransactionContractMapper;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventContractTest {

    @Test
    void transactionEventMatchesCanonicalContract() {
        assertThat(fieldNames(TransactionWorkflowState.class)).containsExactlyInAnyOrderElementsOf(expectedFields());
        assertThat(enumNames(TransactionType.values())).containsExactly("TRANSFER", "DEPOSIT", "WITHDRAW");
        assertThat(enumNames(TransactionStatus.values())).containsExactly(
                "CREATED", "VALIDATION_PENDING", "FRAUD_REVIEW", "BLOCK_MONEY",
                "BLOCK_MONEY_FAILED", "COMPLETED", "CANCELLED", "REVERSED",
                "FAILED", "DEPOSIT_FAILED", "WITHDRAW_FAILED");
    }

    @Test
    void workflowStateRoundTripsThroughGeneratedContract() {
        TransactionContractMapper mapper = new TransactionContractMapper();
        TransactionWorkflowState source = TransactionWorkflowState.builder()
                .eventUUID("event-1")
                .money(new BigDecimal("100.25"))
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.CREATED)
                .build();

        TransactionWorkflowState restored = mapper.fromContract(mapper.toContract(source));

        assertThat(restored.getEventUUID()).isEqualTo("event-1");
        assertThat(restored.getMoney()).isEqualByComparingTo("100.25");
        assertThat(restored.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(restored.getStatus()).isEqualTo(TransactionStatus.CREATED);
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    private Set<String> expectedFields() {
        return Set.of("eventUUID", "keycloakUserUUID", "senderName", "senderSurname", "senderEmail",
                "receiverEmail", "receiverName", "receiverSurname", "senderUserId", "senderIban",
                "receiverUserId", "receiverIban", "money", "transactionType", "description", "status",
                "statusDescription", "error", "errorDescription", "isMoneyBlocked", "userValidation",
                "localDateTime", "tokenDetails");
    }

    private Set<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
