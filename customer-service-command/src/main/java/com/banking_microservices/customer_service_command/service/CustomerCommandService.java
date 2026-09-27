package com.banking_microservices.customer_service_command.service;

import com.banking_microservices.customer_service_command.dto.CreateCustomerCommandRequest;
import com.banking_microservices.customer_service_command.dto.CustomerResponseDto;
import com.banking_microservices.customer_service_command.dto.UpdateContactVerificationRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerProfileRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateKycStatusRequest;
import com.banking_microservices.customer_service_command.dto.UpdateMfaPreferenceRequest;
import com.banking_microservices.customer_service_command.dto.UpdateRiskScoreRequest;
import com.banking_microservices.customer_service_command.dto.UpdateSpecialCustomerRequest;
import com.banking_microservices.customer_service_command.exception.CustomerAlreadyExistsException;
import com.banking_microservices.customer_service_command.exception.CustomerNotFoundException;
import com.banking_microservices.customer_service_command.kafka.CustomerProjectionEventPublisher;
import com.banking_microservices.customer_service_command.model.Customer;
import com.banking_microservices.customer_service_command.repository.CustomerRepository;
import com.banking_microservices.customer_service_command.service.helper.CustomerDeletionService;
import com.banking_microservices.customer_service_command.service.helper.CustomerKycService;
import com.banking_microservices.customer_service_command.service.helper.CustomerMapper;
import com.banking_microservices.customer_service_command.service.helper.CustomerMfaService;
import com.banking_microservices.customer_service_command.service.helper.CustomerProfileService;
import com.banking_microservices.customer_service_command.service.helper.CustomerProjectionEventFactory;
import com.banking_microservices.customer_service_command.service.helper.CustomerRiskService;
import com.banking_microservices.customer_service_command.service.helper.CustomerStatusService;
import com.banking_microservices.customer_service_command.service.helper.CustomerValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final CustomerProjectionEventPublisher projectionEventPublisher;
    private final CustomerProjectionEventFactory projectionEventFactory;
    private final CustomerProfileService profileService;
    private final CustomerStatusService statusService;
    private final CustomerKycService kycService;
    private final CustomerRiskService riskService;
    private final CustomerMfaService mfaService;
    private final CustomerDeletionService deletionService;
    private final CustomerValidator validator;
    private final CustomerMapper mapper;
    private final Supplier<String> currentTime;

    @Transactional
    public CustomerResponseDto createCustomer(CreateCustomerCommandRequest request) {
        log.info("({}) Customer create requested. keycloakId={}, email={}", currentTime.get(), request.keycloakId(), request.email());
        if (customerRepository.existsByKeycloakIdAndDeletedFalse(request.keycloakId())) {
            throw new CustomerAlreadyExistsException("Customer already exists for keycloakId=" + request.keycloakId());
        }
        if (customerRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new CustomerAlreadyExistsException("Customer already exists for email=" + request.email());
        }

        Customer customer = profileService.createModel(request);
        validator.assertScore("riskScore", customer.getRiskScore());
        validator.assertScore("specialCustomerScore", customer.getSpecialCustomerScore());
        validator.assertRange("segmentScore", customer.getSegmentScore(), 1, 10);
        validator.assertMfa(customer.isMfaEnabled(), customer.getMfaMethod());

        Customer saved;
        try {
            saved = customerRepository.save(customer);
        } catch (DataIntegrityViolationException exception) {
            throw new CustomerAlreadyExistsException("Customer identity, email or keycloakId already exists");
        }
        publish(saved, "CUSTOMER_CREATED");
        log.info("({}) Customer created. customerId={}, keycloakId={}", currentTime.get(), saved.getId(), saved.getKeycloakId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateProfile(UpdateCustomerProfileRequest request) {
        Customer customer = getWritable(request.customerId());
        if (request.email() != null && !request.email().equalsIgnoreCase(customer.getEmail())
                && customerRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new CustomerAlreadyExistsException("Customer already exists for email=" + request.email());
        }
        profileService.updateProfile(customer, request);
        Customer saved = customerRepository.save(customer);
        publish(saved, "PROFILE_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateContactVerification(UpdateContactVerificationRequest request) {
        Customer customer = getWritable(request.customerId());
        profileService.updateContactVerification(customer, request);
        Customer saved = customerRepository.save(customer);
        publish(saved, "CONTACT_VERIFICATION_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateStatus(UpdateCustomerStatusRequest request) {
        Customer customer = getWritable(request.customerId());
        statusService.updateStatus(customer, request.status());
        Customer saved = customerRepository.save(customer);
        publish(saved, "STATUS_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateKycStatus(UpdateKycStatusRequest request) {
        Customer customer = getWritable(request.customerId());
        kycService.updateKycStatus(customer, request.kycStatus());
        Customer saved = customerRepository.save(customer);
        publish(saved, "KYC_STATUS_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateRiskScore(UpdateRiskScoreRequest request) {
        Customer customer = getWritable(request.customerId());
        riskService.updateRiskScore(customer, request.riskScore());
        Customer saved = customerRepository.save(customer);
        publish(saved, "RISK_SCORE_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateMfa(UpdateMfaPreferenceRequest request) {
        Customer customer = getWritable(request.customerId());
        mfaService.updateMfa(customer, request.mfaEnabled(), request.mfaMethod());
        Customer saved = customerRepository.save(customer);
        publish(saved, "MFA_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto updateSpecialCustomer(UpdateSpecialCustomerRequest request) {
        Customer customer = getWritable(request.customerId());
        riskService.updateSpecialCustomer(customer, request.specialCustomer(), request.specialCustomerScore());
        Customer saved = customerRepository.save(customer);
        publish(saved, "SPECIAL_CUSTOMER_UPDATED");
        return mapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponseDto softDelete(UUID customerId) {
        Customer customer = getWritable(customerId);
        deletionService.softDelete(customer);
        Customer saved = customerRepository.save(customer);
        publish(saved, "CUSTOMER_SOFT_DELETED");
        return mapper.toResponse(saved);
    }

    public CustomerResponseDto getById(UUID customerId) {
        return customerRepository.findByIdAndDeletedFalse(customerId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for id=" + customerId));
    }

    public CustomerResponseDto getByKeycloakId(UUID keycloakId) {
        return customerRepository.findByKeycloakIdAndDeletedFalse(keycloakId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for keycloakId=" + keycloakId));
    }

    private Customer getWritable(UUID customerId) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for id=" + customerId));
        validator.assertWritable(customer);
        return customer;
    }

    private void publish(Customer customer, String operationType) {
        projectionEventPublisher.publish(projectionEventFactory.create(customer, operationType));
    }
}
