package com.banking_microservices.customer_service_query.dto;

import lombok.Data;

@Data
public class CustomerProjectionEvent {
    private String eventId;
    private String aggregateId;
    private String keycloakId;
    private String realm;
    private String email;
    private boolean emailVerified;
    private String phoneNumber;
    private boolean phoneVerified;
    private String userType;
    private String status;
    private String birthdate;
    private String name;
    private String middleName;
    private String surname;
    private String sex;
    private int riskScore;
    private boolean mfaEnabled;
    private String mfaMethod;
    private String preferredLanguage;
    private boolean specialCustomer;
    private int specialCustomerScore;
    private String kycStatus;
    private String nationalityCode;
    private String nationalIdMasked;
    private String accountStatus;
    private String maritalStatus;
    private String riskClass;
    private String segment;
    private int segmentScore;
    private String politicalExposureStatus;
    private String employmentCategory;
    private AddressDto address;
    private boolean deleted;
    private Long version;
    private String operationType;
    private String occurredAt;
    private String sourceService;
}
