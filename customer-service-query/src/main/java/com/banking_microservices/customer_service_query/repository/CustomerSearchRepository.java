package com.banking_microservices.customer_service_query.repository;

import com.banking_microservices.customer_service_query.model.CustomerSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface CustomerSearchRepository extends ElasticsearchRepository<CustomerSearchDocument, String> {

    List<CustomerSearchDocument> findByEmailContainingOrPhoneNumberContainingOrStatusContainingOrKycStatusContaining(
            String email,
            String phoneNumber,
            String status,
            String kycStatus
    );
}
