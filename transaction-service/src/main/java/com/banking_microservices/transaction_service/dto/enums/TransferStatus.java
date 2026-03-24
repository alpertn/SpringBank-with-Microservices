package com.banking_microservices.transaction_service.dto.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TransferStatus {
    CREATED("İşleminiz oluşturuldu"),
    SENT_TO_FRAUD("Fraud Servisine Gönderildi"),
    SENT_TO_MONEY("Para Servisine Gönderildi"),
    COMPLETED("İşlem Tamamlandı"),
    FAILED("İşlem Başarısız");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
