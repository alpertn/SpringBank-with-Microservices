    package com.banking_microservices.transaction_service.repository;

    import com.banking_microservices.transaction_service.model.TransactionType;
    import com.banking_microservices.transaction_service.model.transaction;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import java.time.LocalDateTime;
    import java.util.List;

    @Repository
    public interface repository extends JpaRepository<transaction, String> {

        List<transaction> findBySenderUserId(String senderUserId);

        List<transaction> findByReceiverUserId(String receiverUserId);

        List<transaction> findByType(TransactionType type);

        List<transaction> findBySenderUserIdOrReceiverUserIdOrderByLocalDateTimeDesc(String senderUserId, String receiverUserId);

        List<transaction> findByErrorTrue();

        List<transaction> findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(LocalDateTime startDate, LocalDateTime endDate);

        List<transaction> findBySenderUserIdAndLocalDateTimeBetween(String senderUserId, LocalDateTime startDate, LocalDateTime endDate);

    }
