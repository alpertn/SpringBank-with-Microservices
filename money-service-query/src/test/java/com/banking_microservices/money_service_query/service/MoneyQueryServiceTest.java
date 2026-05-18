package com.banking_microservices.money_service_query.service;

import com.banking_microservices.money_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.money_service_query.model.MoneyAccountDocument;
import com.banking_microservices.money_service_query.repository.MoneyAccountMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyQueryServiceTest {

    @Mock
    private MoneyAccountMongoRepository mongoRepository;

    private MoneyQueryService moneyQueryService;

    @BeforeEach
    void setUp() {
        moneyQueryService = new MoneyQueryService(mongoRepository);
    }

    @Test
    void getByUserIdReturnsProjectedBalance() {
        when(mongoRepository.findByUserId("receiver-user")).thenReturn(Optional.of(document()));

        var response = moneyQueryService.getByUserId("receiver-user");

        assertThat(response.userId()).isEqualTo("receiver-user");
        assertThat(response.availableBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void searchReturnsMongoProjectionResults() {
        when(mongoRepository.findByUserIdContainingIgnoreCaseOrUserIbanContainingIgnoreCaseOrKeycloakUserUUIDContainingIgnoreCase("receiver", "receiver", "receiver"))
                .thenReturn(List.of(document()));

        assertThat(moneyQueryService.search("receiver")).hasSize(1);
    }

    @Test
    void getByIbanThrowsWhenReadModelIsMissing() {
        when(mongoRepository.findByUserIban("TRMISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moneyQueryService.getByIban("TRMISSING"))
                .isInstanceOf(ReadModelNotFoundException.class);
    }

    private MoneyAccountDocument document() {
        return MoneyAccountDocument.builder()
                .id("account-1")
                .userId("receiver-user")
                .keycloakUserUUID("receiver-keycloak")
                .userIban("TRRECEIVER")
                .availableBalance(new BigDecimal("1000.00"))
                .blockedBalance(new BigDecimal("0.00"))
                .build();
    }
}
