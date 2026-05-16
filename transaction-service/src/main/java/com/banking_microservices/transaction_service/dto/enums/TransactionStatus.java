package com.banking_microservices.transaction_service.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
    CREATED("Isleminiz alindi"),
    VALIDATION_PENDING("Hesap bilgileri dogrulaniyor"),
    FRAUD_REVIEW("Isleminiz inceleniyor"),
    BLOCK_MONEY("Tutar rezerve edildi"),
    BLOCK_MONEY_FAILED("Tutar rezerve edilemedi, islem iptal edildi"),
    COMPLETED("Islem tamamlandi"),
    CANCELLED("Islem iptal edildi"),
    REVERSED("Islem geri alindi"),
    FAILED("Islem basarisiz"),
    DEPOSIT_FAILED("Para yatirma islemi basarisiz"),
    WITHDRAW_FAILED("Para cekme islemi basarisiz");

    private final String description;
}
