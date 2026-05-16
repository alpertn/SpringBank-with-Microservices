package com.banking_microservices.money_service_query.repository;

import com.banking_microservices.money_service_query.model.MoneyAccountSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface MoneyAccountSearchRepository extends ElasticsearchRepository<MoneyAccountSearchDocument, String> {

    List<MoneyAccountSearchDocument> findByUserIdContainingOrUserIbanContainingOrKeycloakUserUUIDContaining(
            String userId,
            String userIban,
            String keycloakUserUUID
    );
}
