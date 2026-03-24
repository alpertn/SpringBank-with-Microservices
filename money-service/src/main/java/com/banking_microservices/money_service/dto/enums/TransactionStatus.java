package com.banking_microservices.money_service.dto.enums;

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

    // Kullanılmadığı için kaldırılan statüler (Referans Amaçlı):
    // VALIDATION_FAILED("Gönderici veya alıcı IBAN bulunamadı ya da hesap bilgileri geçersiz"),
    // INSUFFICIENT_FUNDS("Göndericinin hesabında transfer için yeterli bakiye bulunmuyor"),
    // FRAUD_REJECTED("İşlem fraud veya risk kurallarını ihlal ettiği için reddedildi"),
    // PROCESSING("Transfer işleniyor, gönderici hesabından düşülüp alıcı hesabına aktarılıyor"),
    // KAFKA_ERROR("Servisler arası mesaj iletiminde teknik hata oluştu"),
    // DECLINED("Transfer reddedildi: negatif tutar, aynı hesaba transfer veya iş kuralı ihlali"),
    // REVERSED("İşlem hata nedeniyle geri alındı, bloke edilen tutar hesaba iade edildi");
}
