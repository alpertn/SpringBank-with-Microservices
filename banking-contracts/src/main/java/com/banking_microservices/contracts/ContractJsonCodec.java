package com.banking_microservices.contracts;

import com.banking_microservices.contracts.transaction.v1.SagaWorkflowEvent;
import com.banking_microservices.contracts.transaction.v1.TransactionWorkflowEvent;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

public final class ContractJsonCodec {

    private static final JsonFormat.Printer PRINTER = JsonFormat.printer();
    private static final JsonFormat.Parser PARSER = JsonFormat.parser()
            .ignoringUnknownFields();

    private ContractJsonCodec() {
    }

    public static String toJson(MessageOrBuilder message) {
        try {
            return PRINTER.print(message);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Protobuf contract JSON'a donusturulemedi.", exception);
        }
    }

    public static TransactionWorkflowEvent parseTransaction(String json) {
        try {
            TransactionWorkflowEvent.Builder builder = TransactionWorkflowEvent.newBuilder();
            PARSER.merge(json, builder);
            return builder.build();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Transaction contract JSON okunamadi.", exception);
        }
    }

    public static SagaWorkflowEvent parseSaga(String json) {
        try {
            SagaWorkflowEvent.Builder builder = SagaWorkflowEvent.newBuilder();
            PARSER.merge(json, builder);
            return builder.build();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Saga contract JSON okunamadi.", exception);
        }
    }
}
