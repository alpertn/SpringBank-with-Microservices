package com.banking_microservices.admin_service_command.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admin_query_history")
public class AdminHistoryRecord {

    @Id
    @Column(nullable = false, updatable = false, length = 120)
    private String requestId;

    @Column(length = 255)
    private String adminEmail;

    @Column(length = 255)
    private String adminPasswordMasked;

    @Column(length = 40)
    private String transport;

    @Column(length = 80)
    private String requestType;

    @Column(length = 80)
    private String targetType;

    @Column(length = 255)
    private String targetName;

    @Column(length = 255)
    private String topicName;

    @Column(length = 40)
    private String status;

    private boolean responseReceived;

    @Column(length = 80)
    private String responseType;

    @Column(columnDefinition = "TEXT")
    private String queryText;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime requestedAt;

    private LocalDateTime receivedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
