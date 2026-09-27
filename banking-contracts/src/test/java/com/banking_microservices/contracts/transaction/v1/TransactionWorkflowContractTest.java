package com.banking_microservices.contracts.transaction.v1;

import com.banking_microservices.contracts.ContractJsonCodec;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionWorkflowContractTest {

    @Test
    void transactionContractHasCanonicalFieldsAndEnums() {
        List<String> fields = TransactionWorkflowEvent.getDescriptor().getFields().stream()
                .map(field -> field.getJsonName())
                .toList();

        assertThat(fields).contains("eventUUID", "transactionType", "money", "status", "isMoneyBlocked");
        assertThat(TransactionType.values()).extracting(Enum::name)
                .containsExactly("TRANSACTION_TYPE_UNSPECIFIED", "TRANSFER", "DEPOSIT", "WITHDRAW", "UNRECOGNIZED");
    }

    @Test
    void sagaJsonUsesOnlyCanonicalTransactionField() throws Exception {
        SagaWorkflowEvent event = SagaWorkflowEvent.newBuilder()
                .setSagaId("saga-1")
                .setTransactionEventId("transaction-1")
                .setStatus(SagaStatus.SAGA_STATUS_CREATED)
                .setTransaction(SagaTransactionSnapshot.newBuilder()
                        .setEventId("transaction-1")
                        .setMoney("100.00")
                        .setTransactionType(TransactionType.TRANSFER)
                        .setStatus(TransactionStatus.TRANSACTION_STATUS_COMPLETED))
                .build();

        String json = JsonFormat.printer().print(event);

        assertThat(json).contains("\"transaction\"");
        assertThat(json).doesNotContain("transactionHistory", "transactionEntity");
    }

    @Test
    void codecRoundTripsGeneratedTransactionContract() {
        TransactionWorkflowEvent source = TransactionWorkflowEvent.newBuilder()
                .setEventUuid("event-1")
                .setMoney("125.40")
                .setTransactionType(TransactionType.TRANSFER)
                .setStatus(TransactionStatus.TRANSACTION_STATUS_CREATED)
                .build();

        TransactionWorkflowEvent restored = ContractJsonCodec.parseTransaction(ContractJsonCodec.toJson(source));

        assertThat(restored).isEqualTo(source);
    }
}
