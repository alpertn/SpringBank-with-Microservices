package com.springbank.user_service.model;

import org.iban4j.Iban; // Iban olusturma
import org.iban4j.CountryCode; // Iban olusturma

//        <dependency>
//            <groupId>org.iban4j</groupId>
//            <artifactId>iban4j</artifactId>
//            <version>3.2.3-RELEASE</version>
//        </dependency>

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {

    @Id
    @UuidGenerator
    private String id;
    private String tckimlikNo;
    private String iban;
    private String password;
    @Builder.Default
    private Double customerBalance = 0.00;
    private String customerName;
    private String customerSurname;

    @PrePersist
    public void generateIban(){
        iban = Iban.random(CountryCode.TR).toString();
    }


}
