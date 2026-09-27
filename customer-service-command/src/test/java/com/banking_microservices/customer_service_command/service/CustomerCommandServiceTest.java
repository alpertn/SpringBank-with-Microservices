package com.banking_microservices.customer_service_command.service;

import com.banking_microservices.customer_service_command.dto.CreateCustomerCommandRequest;
import com.banking_microservices.customer_service_command.dto.AddressInput;
import com.banking_microservices.customer_service_command.address.ParsedAddress;
import com.banking_microservices.customer_service_command.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_command.dto.UpdateMfaPreferenceRequest;
import com.banking_microservices.customer_service_command.dto.UpdateRiskScoreRequest;
import com.banking_microservices.customer_service_command.dto.enums.KeycloakRealm;
import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.dto.enums.NationalityCode;
import com.banking_microservices.customer_service_command.dto.enums.Sex;
import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.dto.enums.UserType;
import com.banking_microservices.customer_service_command.exception.CustomerAlreadyExistsException;
import com.banking_microservices.customer_service_command.exception.InvalidCustomerStateException;
import com.banking_microservices.customer_service_command.exception.InvalidRiskScoreException;
import com.banking_microservices.customer_service_command.kafka.CustomerProjectionEventPublisher;
import com.banking_microservices.customer_service_command.model.Customer;
import com.banking_microservices.customer_service_command.repository.CustomerRepository;
import com.banking_microservices.customer_service_command.service.helper.CustomerDeletionService;
import com.banking_microservices.customer_service_command.service.helper.CustomerAddressService;
import com.banking_microservices.customer_service_command.service.helper.CustomerKycService;
import com.banking_microservices.customer_service_command.service.helper.CustomerMapper;
import com.banking_microservices.customer_service_command.service.helper.CustomerMfaService;
import com.banking_microservices.customer_service_command.service.helper.CustomerProfileService;
import com.banking_microservices.customer_service_command.service.helper.CustomerProjectionEventFactory;
import com.banking_microservices.customer_service_command.service.helper.CustomerRiskService;
import com.banking_microservices.customer_service_command.service.helper.CustomerStatusService;
import com.banking_microservices.customer_service_command.service.helper.CustomerValidator;
import com.banking_microservices.customer_service_command.service.helper.NationalIdProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCommandServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerProjectionEventPublisher projectionEventPublisher;

    private CustomerCommandService customerCommandService;
    private UUID keycloakId;

    @BeforeEach
    void setUp() {
        CustomerValidator validator = new CustomerValidator();
        CustomerAddressService addressService = new CustomerAddressService((address, countryCode) ->
                new ParsedAddress("Türkiye", "TR", "İstanbul", "Kadıköy", "Caferağa", "Moda Caddesi",
                        null, "10", null, "2", "5", "34710", "libpostal", "test"));
        customerCommandService = new CustomerCommandService(
                customerRepository,
                projectionEventPublisher,
                new CustomerProjectionEventFactory(),
                new CustomerProfileService(new NationalIdProtector("test-hmac-key-for-customer-service"), addressService),
                new CustomerStatusService(),
                new CustomerKycService(validator),
                new CustomerRiskService(validator),
                new CustomerMfaService(validator),
                new CustomerDeletionService(),
                validator,
                new CustomerMapper(),
                () -> "12:00:00"
        );
        keycloakId = UUID.randomUUID();
    }

    @Test
    void createCustomerPublishesProjection() {
        when(customerRepository.existsByKeycloakIdAndDeletedFalse(keycloakId)).thenReturn(false);
        when(customerRepository.existsByEmailIgnoreCaseAndDeletedFalse("test@bank.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(UUID.randomUUID());
            return customer;
        });

        var response = customerCommandService.createCustomer(createRequest());

        assertThat(response.keycloakId()).isEqualTo(keycloakId);
        assertThat(response.email()).isEqualTo("test@bank.com");
        ArgumentCaptor<CustomerProjectionEvent> captor = ArgumentCaptor.forClass(CustomerProjectionEvent.class);
        verify(projectionEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().operationType()).isEqualTo("CUSTOMER_CREATED");
    }

    @Test
    void createCustomerRejectsDuplicateEmail() {
        when(customerRepository.existsByKeycloakIdAndDeletedFalse(keycloakId)).thenReturn(false);
        when(customerRepository.existsByEmailIgnoreCaseAndDeletedFalse("test@bank.com")).thenReturn(true);

        assertThatThrownBy(() -> customerCommandService.createCustomer(createRequest()))
                .isInstanceOf(CustomerAlreadyExistsException.class);
    }

    @Test
    void updateRiskScoreRejectsOutOfRangeValue() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerCommandService.updateRiskScore(
                UpdateRiskScoreRequest.builder().customerId(customerId).riskScore(101).build()))
                .isInstanceOf(InvalidRiskScoreException.class);
    }

    @Test
    void updateMfaRejectsEnabledWithNoneMethod() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerCommandService.updateMfa(
                UpdateMfaPreferenceRequest.builder().customerId(customerId).mfaEnabled(true).mfaMethod(MfaMethod.NONE).build()))
                .isInstanceOf(InvalidCustomerStateException.class);
    }

    private CreateCustomerCommandRequest createRequest() {
        return CreateCustomerCommandRequest.builder()
                .keycloakId(keycloakId)
                .realm(KeycloakRealm.BANKING)
                .email("test@bank.com")
                .emailVerified(true)
                .userType(UserType.INDIVIDUAL)
                .status(UserStatus.ACTIVE)
                .name("Test")
                .surname("User")
                .sex(Sex.UNSPECIFIED)
                .mfaMethod(MfaMethod.NONE)
                .preferredLanguage("tr")
                .kycStatus(KycStatus.PENDING)
                .nationalityCode(NationalityCode.TR)
                .nationalId("10000000146")
                .segmentScore(1)
                .address(new AddressInput(null, "Moda Caddesi No:10 D:5 Kadıköy İstanbul", "TR"))
                .build();
    }
}
