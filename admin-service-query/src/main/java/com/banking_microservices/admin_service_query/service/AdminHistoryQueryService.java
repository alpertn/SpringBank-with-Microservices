package com.banking_microservices.admin_service_query.service;

import com.banking_microservices.admin_service_query.dto.AdminHistoryReadDto;
import com.banking_microservices.admin_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.admin_service_query.model.AdminHistoryDocument;
import com.banking_microservices.admin_service_query.repository.AdminHistoryMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminHistoryQueryService {

    private final AdminHistoryMongoRepository repository;

    public AdminHistoryReadDto getByRequestId(String requestId) {
        return repository.findById(requestId)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Admin history not found for requestId=" + requestId));
    }

    public List<AdminHistoryReadDto> list(int limit, String keyword) {
        List<AdminHistoryDocument> documents = (keyword == null || keyword.isBlank())
                ? repository.findTop100ByOrderByRequestedAtDesc()
                : repository.findTop100ByAdminEmailContainingIgnoreCaseOrTargetNameContainingIgnoreCaseOrQueryTextContainingIgnoreCaseOrRequestTypeContainingIgnoreCaseOrderByRequestedAtDesc(
                        keyword, keyword, keyword, keyword
                );
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return documents.stream().limit(safeLimit).map(this::toDto).toList();
    }

    private AdminHistoryReadDto toDto(AdminHistoryDocument document) {
        return new AdminHistoryReadDto(
                document.getRequestId(),
                document.getAdminEmail(),
                document.getAdminPasswordMasked(),
                document.getTransport(),
                document.getRequestType(),
                document.getTargetType(),
                document.getTargetName(),
                document.getTopicName(),
                document.getStatus(),
                document.isResponseReceived(),
                document.getResponseType(),
                document.getQueryText(),
                document.getRequestPayload(),
                document.getResponsePayload(),
                document.getErrorMessage(),
                document.getRequestedAt(),
                document.getReceivedAt()
        );
    }
}
