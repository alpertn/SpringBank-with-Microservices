package com.springbank.money_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.iban4j.CountryCode;
import org.iban4j.Iban;

@Entity
@Table(name = "money")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class money {

    @Id
    @UuidGenerator
    private String id;
    private String iban;
    private String userUUID;
    @Builder.Default
    private double balance = 0;

    @PrePersist
    public void generateIban() {
        String newIban = Iban.random(CountryCode.TR).toString();

        iban = newIban;

    }

}
