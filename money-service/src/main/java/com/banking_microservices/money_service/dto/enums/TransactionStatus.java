package com.banking_microservices.money_service.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
    CREATED("Transfer talebi oluşturuldu, işleme kuyruğuna alındı"),
    VALIDATION_PENDING("Gönderici ve alıcı IBAN bilgileri ile hesap varlığı doğrulanıyor"),
    VALIDATION_FAILED("Gönderici veya alıcı IBAN bulunamadı ya da hesap bilgileri geçersiz"),
    INSUFFICIENT_FUNDS("Göndericinin hesabında transfer için yeterli bakiye bulunmuyor"),
    FRAUD_REVIEW("İşlem fraud ve risk analizi için incelemeye alındı"),
    FRAUD_REJECTED("İşlem fraud veya risk kurallarını ihlal ettiği için reddedildi"),
    FUNDS_BLOCKED("Transfer tutarı gönderici hesabından bloke edildi, işleme devam ediliyor"),
    FUNDS_BLOCK_FAILED("Gönderici hesabından tutar bloke edilemedi, işlem iptal edildi"),
    PROCESSING("Transfer işleniyor, gönderici hesabından düşülüp alıcı hesabına aktarılıyor"),
    COMPLETED("Transfer başarıyla tamamlandı, tutar alıcı hesabına yansıdı"),
    FAILED("Para çekme veya yatırma adımında teknik hata oluştu, işlem başarısız"),
    KAFKA_ERROR("Servisler arası mesaj iletiminde teknik hata oluştu"),
    DECLINED("Transfer reddedildi: negatif tutar, aynı hesaba transfer veya iş kuralı ihlali"),
    REVERSED("İşlem hata nedeniyle geri alındı, bloke edilen tutar hesaba iade edildi");

    private final String description;
}
