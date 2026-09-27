package com.banking_microservices.money_service.kafka;

import com.banking_microservices.contracts.transaction.v1.SagaWorkflowEvent;
import com.banking_microservices.contracts.transaction.v1.TransactionWorkflowEvent;
import com.banking_microservices.money_service.model.workflow.TransactionWorkflowState;
import com.banking_microservices.money_service.dto.SagaEventsDto;
import com.banking_microservices.money_service.dto.SagaTransactionSnapshot;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.dto.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

@Component
public class TransactionContractMapper {

    public TransactionWorkflowEvent toContract(TransactionWorkflowState source) {
        TransactionWorkflowEvent.Builder target = TransactionWorkflowEvent.newBuilder()
                .setEventUuid(text(source.getEventUUID()))
                .setMoney(decimal(source.getMoney()))
                .setTransactionType(toContract(source.getTransactionType()))
                .setStatus(toContract(source.getStatus()))
                .setError(Boolean.TRUE.equals(source.getError()))
                .setMoneyBlocked(Boolean.TRUE.equals(source.getIsMoneyBlocked()))
                .setUserValidation(Boolean.TRUE.equals(source.getUserValidation()));
        optional(target::setKeycloakUserUuid, source.getKeycloakUserUUID());
        optional(target::setSenderName, source.getSenderName());
        optional(target::setSenderSurname, source.getSenderSurname());
        optional(target::setSenderEmail, source.getSenderEmail());
        optional(target::setReceiverEmail, source.getReceiverEmail());
        optional(target::setReceiverName, source.getReceiverName());
        optional(target::setReceiverSurname, source.getReceiverSurname());
        optional(target::setSenderUserId, source.getSenderUserId());
        optional(target::setSenderIban, source.getSenderIban());
        optional(target::setReceiverUserId, source.getReceiverUserId());
        optional(target::setReceiverIban, source.getReceiverIban());
        optional(target::setDescription, source.getDescription());
        optional(target::setStatusDescription, source.getStatusDescription());
        optional(target::setErrorDescription, source.getErrorDescription());
        optional(target::setOccurredAt, source.getLocalDateTime() == null ? null : source.getLocalDateTime().toString());
        return target.build();
    }

    public TransactionWorkflowState fromContract(TransactionWorkflowEvent source) {
        return TransactionWorkflowState.builder()
                .eventUUID(source.getEventUuid())
                .keycloakUserUUID(source.hasKeycloakUserUuid() ? source.getKeycloakUserUuid() : null)
                .senderName(source.hasSenderName() ? source.getSenderName() : null)
                .senderSurname(source.hasSenderSurname() ? source.getSenderSurname() : null)
                .senderEmail(source.hasSenderEmail() ? source.getSenderEmail() : null)
                .receiverEmail(source.hasReceiverEmail() ? source.getReceiverEmail() : null)
                .receiverName(source.hasReceiverName() ? source.getReceiverName() : null)
                .receiverSurname(source.hasReceiverSurname() ? source.getReceiverSurname() : null)
                .senderUserId(source.hasSenderUserId() ? source.getSenderUserId() : null)
                .senderIban(source.hasSenderIban() ? source.getSenderIban() : null)
                .receiverUserId(source.hasReceiverUserId() ? source.getReceiverUserId() : null)
                .receiverIban(source.hasReceiverIban() ? source.getReceiverIban() : null)
                .money(source.getMoney().isBlank() ? null : new BigDecimal(source.getMoney()))
                .transactionType(fromContract(source.getTransactionType()))
                .description(source.hasDescription() ? source.getDescription() : null)
                .status(fromContract(source.getStatus()))
                .statusDescription(source.hasStatusDescription() ? source.getStatusDescription() : null)
                .error(source.getError())
                .errorDescription(source.hasErrorDescription() ? source.getErrorDescription() : null)
                .isMoneyBlocked(source.getMoneyBlocked())
                .userValidation(source.getUserValidation())
                .localDateTime(parseDate(source.hasOccurredAt() ? source.getOccurredAt() : null))
                .build();
    }

    public SagaWorkflowEvent toContract(SagaEventsDto source) {
        var target = SagaWorkflowEvent.newBuilder()
                .setSagaId(text(source.getUUID()))
                .setTransactionEventId(text(source.getKafkaEventUUID()))
                .setStatus(com.banking_microservices.contracts.transaction.v1.SagaStatus.valueOf(
                        "SAGA_STATUS_" + source.getStatus().name()));
        optional(target::setErrorDescription, source.getErrorDescripton());
        if (source.getTransaction() != null) target.setTransaction(toContract(source.getTransaction()));
        return target.build();
    }

    public SagaEventsDto fromContract(SagaWorkflowEvent source) {
        return SagaEventsDto.builder()
                .UUID(source.getSagaId())
                .kafkaEventUUID(source.getTransactionEventId())
                .status(com.banking_microservices.money_service.dto.enums.SagaStatus.valueOf(
                        source.getStatus().name().replace("SAGA_STATUS_", "")))
                .errorDescripton(source.hasErrorDescription() ? source.getErrorDescription() : null)
                .transaction(source.hasTransaction() ? fromContract(source.getTransaction()) : null)
                .build();
    }

    private com.banking_microservices.contracts.transaction.v1.SagaTransactionSnapshot toContract(
            SagaTransactionSnapshot source) {
        var target = com.banking_microservices.contracts.transaction.v1.SagaTransactionSnapshot.newBuilder()
                .setEventId(text(source.getEventId()))
                .setMoney(decimal(source.getMoney()))
                .setTransactionType(toContract(TransactionType.valueOf(source.getTransactionType())))
                .setError(Boolean.TRUE.equals(source.getError()))
                .setUserValidation(Boolean.TRUE.equals(source.getUserValidation()))
                .setMoneyBlocked(Boolean.TRUE.equals(source.getIsMoneyBlocked()))
                .setStatus(toContract(TransactionStatus.valueOf(source.getStatus())));
        optional(target::setId, source.getId());
        optional(target::setReceiverName, source.getReceiverName());
        optional(target::setSenderName, source.getSenderName());
        optional(target::setSenderSurname, source.getSenderSurname());
        optional(target::setSenderEmail, source.getSenderEmail());
        optional(target::setReceiverEmail, source.getReceiverEmail());
        optional(target::setReceiverSurname, source.getReceiverSurname());
        optional(target::setSenderUserId, source.getSenderUserId());
        optional(target::setReceiverUserId, source.getReceiverUserId());
        optional(target::setSenderIban, source.getSenderIban());
        optional(target::setReceiverIban, source.getReceiverIban());
        optional(target::setDescription, source.getDescription());
        optional(target::setOccurredAt, source.getLocalDateTime() == null ? null : source.getLocalDateTime().toString());
        optional(target::setErrorDescription, source.getErrorDescription());
        return target.build();
    }

    private SagaTransactionSnapshot fromContract(
            com.banking_microservices.contracts.transaction.v1.SagaTransactionSnapshot source) {
        return SagaTransactionSnapshot.builder()
                .id(source.hasId() ? source.getId() : null)
                .eventId(source.getEventId())
                .receiverName(source.hasReceiverName() ? source.getReceiverName() : null)
                .senderName(source.hasSenderName() ? source.getSenderName() : null)
                .senderSurname(source.hasSenderSurname() ? source.getSenderSurname() : null)
                .senderEmail(source.hasSenderEmail() ? source.getSenderEmail() : null)
                .receiverEmail(source.hasReceiverEmail() ? source.getReceiverEmail() : null)
                .receiverSurname(source.hasReceiverSurname() ? source.getReceiverSurname() : null)
                .senderUserId(source.hasSenderUserId() ? source.getSenderUserId() : null)
                .receiverUserId(source.hasReceiverUserId() ? source.getReceiverUserId() : null)
                .senderIban(source.hasSenderIban() ? source.getSenderIban() : null)
                .receiverIban(source.hasReceiverIban() ? source.getReceiverIban() : null)
                .money(source.getMoney().isBlank() ? null : new BigDecimal(source.getMoney()))
                .transactionType(fromContract(source.getTransactionType()).name())
                .description(source.hasDescription() ? source.getDescription() : null)
                .localDateTime(parseDate(source.hasOccurredAt() ? source.getOccurredAt() : null))
                .error(source.getError())
                .errorDescription(source.hasErrorDescription() ? source.getErrorDescription() : null)
                .userValidation(source.getUserValidation())
                .isMoneyBlocked(source.getMoneyBlocked())
                .status(fromContract(source.getStatus()).name())
                .build();
    }

    private com.banking_microservices.contracts.transaction.v1.TransactionType toContract(TransactionType value) {
        return value == null
                ? com.banking_microservices.contracts.transaction.v1.TransactionType.TRANSACTION_TYPE_UNSPECIFIED
                : com.banking_microservices.contracts.transaction.v1.TransactionType.valueOf(value.name());
    }

    private TransactionType fromContract(com.banking_microservices.contracts.transaction.v1.TransactionType value) {
        return value == com.banking_microservices.contracts.transaction.v1.TransactionType.TRANSACTION_TYPE_UNSPECIFIED
                || value == com.banking_microservices.contracts.transaction.v1.TransactionType.UNRECOGNIZED
                ? TransactionType.TRANSFER : TransactionType.valueOf(value.name());
    }

    private com.banking_microservices.contracts.transaction.v1.TransactionStatus toContract(TransactionStatus value) {
        return value == null
                ? com.banking_microservices.contracts.transaction.v1.TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED
                : com.banking_microservices.contracts.transaction.v1.TransactionStatus.valueOf("TRANSACTION_STATUS_" + value.name());
    }

    private TransactionStatus fromContract(com.banking_microservices.contracts.transaction.v1.TransactionStatus value) {
        return value == com.banking_microservices.contracts.transaction.v1.TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED
                || value == com.banking_microservices.contracts.transaction.v1.TransactionStatus.UNRECOGNIZED
                ? TransactionStatus.CREATED
                : TransactionStatus.valueOf(value.name().replace("TRANSACTION_STATUS_", ""));
    }

    private void optional(Consumer<String> setter, String value) {
        if (value != null) setter.accept(value);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private LocalDateTime parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
