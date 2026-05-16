package com.banking_microservices.money_service_command.service;

import com.banking_microservices.money_service_command.dto.BalanceCommandRequest;
import com.banking_microservices.money_service_command.dto.BlockMoneyCommandRequest;
import com.banking_microservices.money_service_command.dto.CreateMoneyAccountRequest;
import com.banking_microservices.money_service_command.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_command.dto.TransferCommandRequest;
import com.banking_microservices.money_service_command.exception.InsufficientFundsException;
import com.banking_microservices.money_service_command.exception.InvalidAmountException;
import com.banking_microservices.money_service_command.kafka.MoneyProjectionEventPublisher;
import com.banking_microservices.money_service_command.model.UserMoney;
import com.banking_microservices.money_service_command.repository.UserMoneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyCommandServiceTest {

    @Mock
    private UserMoneyRepository userMoneyRepository;

    @Mock
    private MoneyProjectionEventPublisher projectionEventPublisher;

    private MoneyCommandService moneyCommandService;
    private Map<String, UserMoney> byUserId;
    private Map<String, UserMoney> byIban;

    @BeforeEach
    void setUp() {
        byUserId = new HashMap<>();
        byIban = new HashMap<>();
        moneyCommandService = new MoneyCommandService(userMoneyRepository, projectionEventPublisher, () -> "12:00:00");
    }

    @Test
    void transferFlowUpdatesBalancesAfterDepositBlockAndExecuteTransfer() {
        stubSave();
        UserMoney sender = account("sender-user", "sender-keycloak", "TRSENDER", "0.00", "0.00");
        UserMoney receiver = account("receiver-user", "receiver-keycloak", "TRRECEIVER", "0.00", "0.00");
        saveAccount(sender);
        saveAccount(receiver);
        when(userMoneyRepository.findByUserId("sender-user")).thenReturn(Optional.of(sender));
        when(userMoneyRepository.findByUserIban("TRSENDER")).thenReturn(Optional.of(sender));
        when(userMoneyRepository.findByUserIban("TRRECEIVER")).thenReturn(Optional.of(receiver));

        moneyCommandService.deposit(new BalanceCommandRequest("sender-user", new BigDecimal("5000.00")));
        moneyCommandService.blockMoney(new BlockMoneyCommandRequest("TRSENDER", new BigDecimal("1000.00")));
        moneyCommandService.executeTransfer(new TransferCommandRequest("TRSENDER", "TRRECEIVER", new BigDecimal("1000.00")));

        assertThat(sender.getMoney()).isEqualByComparingTo("4000.00");
        assertThat(sender.getBlockedMoney()).isEqualByComparingTo("0.00");
        assertThat(receiver.getMoney()).isEqualByComparingTo("1000.00");

        ArgumentCaptor<MoneyProjectionEvent> eventCaptor = ArgumentCaptor.forClass(MoneyProjectionEvent.class);
        verify(projectionEventPublisher, org.mockito.Mockito.times(4)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(MoneyProjectionEvent::operationType)
                .containsExactly("DEPOSIT", "BLOCK_MONEY", "TRANSFER_SENT", "TRANSFER_RECEIVED");
    }

    @Test
    void createAccountPublishesAccountCreatedProjection() {
        stubSave();
        when(userMoneyRepository.existsByUserId("user-1")).thenReturn(false);
        when(userMoneyRepository.existsByKeycloakUserUUID("keycloak-1")).thenReturn(false);

        var response = moneyCommandService.createAccount(new CreateMoneyAccountRequest("user-1", "keycloak-1"));

        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.keycloakUserUUID()).isEqualTo("keycloak-1");
        assertThat(response.userIban()).startsWith("TR");
        ArgumentCaptor<MoneyProjectionEvent> eventCaptor = ArgumentCaptor.forClass(MoneyProjectionEvent.class);
        verify(projectionEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().operationType()).isEqualTo("ACCOUNT_CREATED");
    }

    @Test
    void withdrawRejectsInsufficientFundsAndInvalidAmounts() {
        UserMoney sender = account("sender-user", "sender-keycloak", "TRSENDER", "50.00", "0.00");
        when(userMoneyRepository.findByUserId("sender-user")).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> moneyCommandService.withdraw(new BalanceCommandRequest("sender-user", new BigDecimal("100.00"))))
                .isInstanceOf(InsufficientFundsException.class);
        assertThatThrownBy(() -> moneyCommandService.deposit(new BalanceCommandRequest("sender-user", BigDecimal.ZERO)))
                .isInstanceOf(InvalidAmountException.class);
    }

    private void stubSave() {
        when(userMoneyRepository.save(any(UserMoney.class))).thenAnswer(invocation -> {
            UserMoney account = invocation.getArgument(0);
            if (account.getId() == null) {
                account.setId("account-" + (byUserId.size() + 1));
            }
            byUserId.put(account.getUserId(), account);
            byIban.put(account.getUserIban(), account);
            return account;
        });
    }

    private UserMoney account(String userId, String keycloakUuid, String iban, String money, String blockedMoney) {
        return UserMoney.builder()
                .id("account-" + userId)
                .userId(userId)
                .keycloakUserUUID(keycloakUuid)
                .userIban(iban)
                .money(new BigDecimal(money))
                .blockedMoney(new BigDecimal(blockedMoney))
                .build();
    }

    private void saveAccount(UserMoney account) {
        byUserId.put(account.getUserId(), account);
        byIban.put(account.getUserIban(), account);
    }
}
