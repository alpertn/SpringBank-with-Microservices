package com.banking_microservices.transaction_service.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
    CREATED("İşleminiz alındı"),
    VALIDATION_PENDING("Hesap bilgileri doğrulanıyor"),
    FRAUD_REVIEW("İşleminiz inceleniyor"),
    BLOCK_MONEY("Tutar rezerve edildi"),
    BLOCK_MONEY_FAILED("Tutar rezerve edilemedi, işlem iptal edildi"),
    COMPLETED("İşlem tamamlandı"),
    FAILED("İşlem başarısız"),
    DEPOSIT_FAILED("Para yatırma işlemi başarısız"),
    WITHDRAW_FAILED("Para çekme işlemi başarısız");

    private final String description;
}
