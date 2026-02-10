package com.banking_microservices.transaction_service.repository;

import com.banking_microservices.transaction_service.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findBySenderUserId(String senderUserId);

    List<TransactionEntity> findByReceiverUserId(String receiverUserId);

    List<TransactionEntity> findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(String senderUserId,
            String receiverUserId);

    List<TransactionEntity> findByErrorTrue();

    List<TransactionEntity> findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(LocalDateTime startDate,
            LocalDateTime endDate);

    List<TransactionEntity> findBySenderUserIdAndLocalDateTimeBetween(String senderUserId, LocalDateTime startDate,
            LocalDateTime endDate);

    boolean existsByEventId(String eventid);

}
