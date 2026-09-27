package com.banking_microservices.customer_service_query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "customer-profiles", createIndex = false)
public class CustomerSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String keycloakId;

    @Field(type = FieldType.Keyword)
    private String realm;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String phoneNumber;

    @Field(type = FieldType.Keyword)
    private String userType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String middleName;

    @Field(type = FieldType.Text)
    private String surname;

    @Field(type = FieldType.Keyword)
    private String kycStatus;

    @Field(type = FieldType.Keyword)
    private String nationalityCode;

    @Field(type = FieldType.Keyword)
    private String accountStatus;

    @Field(type = FieldType.Keyword)
    private String riskClass;

    @Field(type = FieldType.Keyword)
    private String segment;

    @Field(type = FieldType.Integer)
    private int segmentScore;

    @Field(type = FieldType.Keyword)
    private String politicalExposureStatus;

    @Field(type = FieldType.Text)
    private String addressText;

    @Field(type = FieldType.Integer)
    private int riskScore;

    @Field(type = FieldType.Boolean)
    private boolean specialCustomer;

    @Field(type = FieldType.Boolean)
    private boolean deleted;

    @Field(type = FieldType.Keyword)
    private String lastOperationType;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime lastSyncedAt;
}
