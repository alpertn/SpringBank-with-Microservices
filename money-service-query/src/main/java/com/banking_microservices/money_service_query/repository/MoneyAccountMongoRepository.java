package com.banking_microservices.money_service_query.repository;

import com.banking_microservices.money_service_query.model.MoneyAccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MoneyAccountMongoRepository extends MongoRepository<MoneyAccountDocument, String> {

    Optional<MoneyAccountDocument> findByUserId(String userId);

    Optional<MoneyAccountDocument> findByUserIban(String userIban);
}
