package com.banking_microservices.customer_service_query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import com.banking_microservices.customer_service_query.dto.AddressDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_profiles")
public class CustomerDocument {
    @Id
    private String id;
    private String keycloakId;
    private String realm;
    private String email;
    private boolean emailVerified;
    private String phoneNumber;
    private boolean phoneVerified;
    private String userType;
    private String status;
    private LocalDateTime birthdate;
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
    private String lastOperationType;
    private LocalDateTime lastSyncedAt;
}
