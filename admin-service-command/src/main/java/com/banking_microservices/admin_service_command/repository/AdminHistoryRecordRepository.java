package com.banking_microservices.admin_service_command.repository;

import com.banking_microservices.admin_service_command.model.AdminHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminHistoryRecordRepository extends JpaRepository<AdminHistoryRecord, String> {
}
