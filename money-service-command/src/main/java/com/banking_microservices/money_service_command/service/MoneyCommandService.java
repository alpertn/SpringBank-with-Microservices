package com.banking_microservices.money_service_command.service;

import com.banking_microservices.money_service_command.dto.*;
import com.banking_microservices.money_service_command.exception.AccountAlreadyExistsException;
import com.banking_microservices.money_service_command.exception.AccountNotFoundException;
import com.banking_microservices.money_service_command.exception.InsufficientFundsException;
import com.banking_microservices.money_service_command.exception.InvalidAmountException;
import com.banking_microservices.money_service_command.kafka.MoneyProjectionEventPublisher;
import com.banking_microservices.money_service_command.model.UserMoney;
import com.banking_microservices.money_service_command.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyCommandService {

    // CQRS write-side:
    // Tum command islemleri burada calisir ve source of truth Postgres'tir.
    // Her basarili write sonrasi projection event'i Kafka'ya basilir.
    private final UserMoneyRepository userMoneyRepository;
    private final MoneyProjectionEventPublisher projectionEventPublisher;
    private final Supplier<String> currentTime;

    @Transactional
    public MoneyAccountResponseDto createAccount(CreateMoneyAccountRequest request) {
        if (userMoneyRepository.existsByUserId(request.userId()) || userMoneyRepository.existsByKeycloakUserUUID(request.keycloakUserUUID())) {
            throw new AccountAlreadyExistsException("Money account already exists for userId=" + request.userId());
        }

        UserMoney saved = userMoneyRepository.save(UserMoney.builder()
                .userId(request.userId())
                .keycloakUserUUID(request.keycloakUserUUID())
                .userIban(generateIban())
                .build());

        publishProjection(saved, "ACCOUNT_CREATED");
        log.info("({}) Account created. accountId={}, userId={}", currentTime.get(), saved.getId(), saved.getUserId());
        return toResponse(saved);
    }

    @Transactional
    public MoneyAccountResponseDto deposit(BalanceCommandRequest request) {
        validateAmount(request.amount());
        UserMoney account = userMoneyRepository.findByUserId(request.userId())
                .orElseThrow(() -> new AccountNotFoundException("Money account not found for userId=" + request.userId()));
        account.setMoney(account.getMoney().add(request.amount()));
        UserMoney saved = userMoneyRepository.save(account);
        publishProjection(saved, "DEPOSIT");
        return toResponse(saved);
    }

    @Transactional
    public MoneyAccountResponseDto withdraw(BalanceCommandRequest request) {
        validateAmount(request.amount());
        UserMoney account = userMoneyRepository.findByUserId(request.userId())
                .orElseThrow(() -> new AccountNotFoundException("Money account not found for userId=" + request.userId()));
        ensureFunds(account.getMoney(), request.amount(), account.getUserId());
        account.setMoney(account.getMoney().subtract(request.amount()));
        UserMoney saved = userMoneyRepository.save(account);
        publishProjection(saved, "WITHDRAW");
        return toResponse(saved);
    }

    @Transactional
    public MoneyAccountResponseDto blockMoney(BlockMoneyCommandRequest request) {
        validateAmount(request.amount());
        UserMoney account = userMoneyRepository.findByUserIban(request.senderIban())
                .orElseThrow(() -> new AccountNotFoundException("Money account not found for iban=" + request.senderIban()));
        ensureFunds(account.getMoney(), request.amount(), account.getUserIban());
        account.setMoney(account.getMoney().subtract(request.amount()));
        account.setBlockedMoney(account.getBlockedMoney().add(request.amount()));
        UserMoney saved = userMoneyRepository.save(account);
        publishProjection(saved, "BLOCK_MONEY");
        return toResponse(saved);
    }

    @Transactional
    public MoneyAccountResponseDto executeTransfer(TransferCommandRequest request) {
        validateAmount(request.amount());
        if (request.senderIban().equals(request.receiverIban())) {
            throw new InvalidAmountException("Sender and receiver iban must be different");
        }

        UserMoney sender = userMoneyRepository.findByUserIban(request.senderIban())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found for iban=" + request.senderIban()));
        UserMoney receiver = userMoneyRepository.findByUserIban(request.receiverIban())
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found for iban=" + request.receiverIban()));

        BigDecimal availableBlocked = sender.getBlockedMoney();
        if (availableBlocked.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Blocked funds are not enough for sender iban=" + request.senderIban());
        }

        sender.setBlockedMoney(sender.getBlockedMoney().subtract(request.amount()));
        receiver.setMoney(receiver.getMoney().add(request.amount()));

        UserMoney savedSender = userMoneyRepository.save(sender);
        UserMoney savedReceiver = userMoneyRepository.save(receiver);

        publishProjection(savedSender, "TRANSFER_SENT");
        publishProjection(savedReceiver, "TRANSFER_RECEIVED");
        return toResponse(savedSender);
    }

    private void publishProjection(UserMoney account, String operationType) {
        // Bu method CQRS kopus noktasidir:
        // 1) Write modeli Postgres'te commit edilir
        // 2) Read modeli guncellensin diye Kafka'ya event basilir
        String eventId = UUID.randomUUID().toString();
        MoneyProjectionEvent event = MoneyProjectionEvent.builder()
                .eventId(eventId)
                .aggregateId(account.getId())
                .userId(account.getUserId())
                .keycloakUserUUID(account.getKeycloakUserUUID())
                .userIban(account.getUserIban())
                .availableBalance(account.getMoney())
                .blockedBalance(account.getBlockedMoney())
                .operationType(operationType)
                .occurredAt(LocalDateTime.now())
                .sourceService("money-service-command")
                .build();

        projectionEventPublisher.publish(event);
    }

    private MoneyAccountResponseDto toResponse(UserMoney account) {
        return new MoneyAccountResponseDto(
                account.getId(),
                account.getUserId(),
                account.getKeycloakUserUUID(),
                account.getUserIban(),
                account.getMoney(),
                account.getBlockedMoney()
        );
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }

    private void ensureFunds(BigDecimal balance, BigDecimal amount, String reference) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for reference=" + reference);
        }
    }

    private String generateIban() {
        // Burasi simdilik basit bir uretec.
        // Gercek bankacilik senaryosunda checksum ve ulke/banka kurallari daha kati uygulanmali.
        return "TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}
