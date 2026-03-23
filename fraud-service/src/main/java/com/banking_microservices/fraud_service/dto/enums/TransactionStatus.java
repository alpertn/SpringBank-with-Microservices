package com.banking_microservices.fraud_service.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
    CREATED("İşleminiz alındı"),
    VALIDATION_PENDING("Hesap bilgileri doğrulanıyor"),
    VALIDATION_FAILED("Hesap bilgileri doğrulanamadı"),
    INSUFFICIENT_FUNDS("Yetersiz bakiye"),
    FRAUD_REVIEW("İşleminiz inceleniyor"),
    FRAUD_REJECTED("İşlem güvenlik kontrolünden geçemedi"),
    BLOCK_MONEY("Tutar rezerve edildi"),
    BLOCK_MONEY_FAILED("Tutar rezerve edilemedi, işlem iptal edildi"),
    PROCESSING("Transfer işleniyor"),
    COMPLETED("İşlem tamamlandı"),
    FAILED("İşlem başarısız"),
    KAFKA_ERROR("Teknik bir hata oluştu"),
    DECLINED("İşlem reddedildi"),
    REVERSED("İşlem geri alındı");

    private final String description;
}
