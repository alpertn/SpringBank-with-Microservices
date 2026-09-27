package com.banking_microservices.customer_service_command.model;

import com.banking_microservices.customer_service_command.domain.BaseEntity;
import com.banking_microservices.customer_service_command.dto.enums.KeycloakRealm;
import com.banking_microservices.customer_service_command.dto.enums.AccountStatus;
import com.banking_microservices.customer_service_command.dto.enums.CustomerSegment;
import com.banking_microservices.customer_service_command.dto.enums.EmploymentCategory;
import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.MaritalStatus;
import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.dto.enums.NationalityCode;
import com.banking_microservices.customer_service_command.dto.enums.PoliticalExposureStatus;
import com.banking_microservices.customer_service_command.dto.enums.RiskClass;
import com.banking_microservices.customer_service_command.dto.enums.Sex;
import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.dto.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_keycloak_id", columnList = "keycloak_id"),
                @Index(name = "idx_customers_email", columnList = "email"),
                @Index(name = "idx_customers_status", columnList = "status"),
                @Index(name = "idx_customers_kyc_status", columnList = "kyc_status"),
                @Index(name = "idx_customers_national_id_hash", columnList = "national_id_hash"),
                @Index(name = "idx_customers_segment", columnList = "segment"),
                @Index(name = "idx_customers_deleted", columnList = "is_deleted")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private UUID keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private KeycloakRealm realm = KeycloakRealm.BANKING;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 40)
    @Builder.Default
    private UserType userType = UserType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "birthdate")
    private LocalDateTime birthdate;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String surname;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    @Builder.Default
    private Sex sex = Sex.UNSPECIFIED;

    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private int riskScore = 0;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", nullable = false, length = 40)
    @Builder.Default
    private MfaMethod mfaMethod = MfaMethod.NONE;

    @Column(name = "preferred_language", nullable = false, length = 10)
    @Builder.Default
    private String preferredLanguage = "tr";

    @Column(name = "special_customer", nullable = false)
    @Builder.Default
    private boolean specialCustomer = false;

    @Column(name = "special_customer_score", nullable = false)
    @Builder.Default
    private int specialCustomerScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 40)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality_code", nullable = false, length = 20)
    @Builder.Default
    private NationalityCode nationalityCode = NationalityCode.TR;

    @Column(name = "national_id_hash", nullable = false, unique = true, length = 64)
    private String nationalIdHash;

    @Column(name = "national_id_masked", nullable = false, length = 20)
    private String nationalIdMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 40)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_ACTIVATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false, length = 30)
    @Builder.Default
    private MaritalStatus maritalStatus = MaritalStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_class", nullable = false, length = 20)
    @Builder.Default
    private RiskClass riskClass = RiskClass.RISK_1;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false, length = 40)
    @Builder.Default
    private CustomerSegment segment = CustomerSegment.STANDARD;

    @Column(name = "segment_score", nullable = false)
    @Builder.Default
    private int segmentScore = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "political_exposure_status", nullable = false, length = 40)
    @Builder.Default
    private PoliticalExposureStatus politicalExposureStatus = PoliticalExposureStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_category", nullable = false, length = 40)
    @Builder.Default
    private EmploymentCategory employmentCategory = EmploymentCategory.OTHER;

    @Embedded
    private CustomerAddress address;
}
