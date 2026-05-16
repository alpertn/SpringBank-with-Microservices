package com.banking_microservices.money_service_query.service;

import com.banking_microservices.money_service_query.dto.MoneyAccountReadDto;
import com.banking_microservices.money_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.money_service_query.model.MoneyAccountDocument;
import com.banking_microservices.money_service_query.model.MoneyAccountSearchDocument;
import com.banking_microservices.money_service_query.repository.MoneyAccountMongoRepository;
import com.banking_microservices.money_service_query.repository.MoneyAccountSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoneyQueryService {

    // Query tarafi write database'e gitmez.
    // Tum okumalar projection verisi uzerinden MongoDB/Elasticsearch tarafindan cevaplanir.
    private final MoneyAccountMongoRepository mongoRepository;
    private final MoneyAccountSearchRepository searchRepository;

    public MoneyAccountReadDto getById(String id) {
        return mongoRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Money read model not found for id=" + id));
    }

    public MoneyAccountReadDto getByUserId(String userId) {
        return mongoRepository.findByUserId(userId)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Money read model not found for userId=" + userId));
    }

    public MoneyAccountReadDto getByIban(String iban) {
        return mongoRepository.findByUserIban(iban)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Money read model not found for iban=" + iban));
    }

    public List<MoneyAccountReadDto> search(String keyword) {
        return searchRepository.findByUserIdContainingOrUserIbanContainingOrKeycloakUserUUIDContaining(keyword, keyword, keyword)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private MoneyAccountReadDto toDto(MoneyAccountDocument document) {
        return new MoneyAccountReadDto(
                document.getId(),
                document.getUserId(),
                document.getKeycloakUserUUID(),
                document.getUserIban(),
                document.getAvailableBalance(),
                document.getBlockedBalance()
        );
    }

    private MoneyAccountReadDto toDto(MoneyAccountSearchDocument document) {
        return new MoneyAccountReadDto(
                document.getId(),
                document.getUserId(),
                document.getKeycloakUserUUID(),
                document.getUserIban(),
                document.getAvailableBalance(),
                document.getBlockedBalance()
        );
    }
}
