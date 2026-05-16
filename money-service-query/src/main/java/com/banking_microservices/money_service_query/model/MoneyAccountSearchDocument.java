package com.banking_microservices.money_service_query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "money-accounts", createIndex = false)
public class MoneyAccountSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String keycloakUserUUID;

    @Field(type = FieldType.Keyword)
    private String userIban;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal availableBalance;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal blockedBalance;

    @Field(type = FieldType.Keyword)
    private String lastOperationType;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime lastSyncedAt;
}
