package com.banking_microservices.fraud_service.dto.enums;

/**
 * Kafka mesajlarinda hangi adimda oldugunu belirtir.
 * Hata topicine gonderilen mesajlarda da bu enum kullanilir.
 * Transaction-Service bu bilgiyi okuyarak islemin hangi adimda
 * hata aldigini kayit altina alir.
 */
public enum KafkaStepType {
    TRANSACTION_CREATED,
    BLOCK_MONEY,
    FRAUD_CHECK,
    MONEY_TRANSFER,
    UNBLOCK_MONEY,
    DEPOSIT,
    WITHDRAW,
    CREATE_USER,
    USERNAME_VALIDATION
}
