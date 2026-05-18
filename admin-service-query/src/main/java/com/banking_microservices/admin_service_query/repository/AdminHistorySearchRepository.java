package com.banking_microservices.admin_service_query.repository;

import com.banking_microservices.admin_service_query.model.AdminHistorySearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AdminHistorySearchRepository extends ElasticsearchRepository<AdminHistorySearchDocument, String> {
}
