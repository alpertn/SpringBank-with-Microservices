package com.banking_microservices.admin_service_query.repository;

import com.banking_microservices.admin_service_query.model.AdminHistoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AdminHistoryMongoRepository extends MongoRepository<AdminHistoryDocument, String> {

    List<AdminHistoryDocument> findTop100ByOrderByRequestedAtDesc();

    List<AdminHistoryDocument> findTop100ByAdminEmailContainingIgnoreCaseOrTargetNameContainingIgnoreCaseOrQueryTextContainingIgnoreCaseOrRequestTypeContainingIgnoreCaseOrderByRequestedAtDesc(
            String adminEmail,
            String targetName,
            String queryText,
            String requestType
    );
}
