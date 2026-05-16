package com.banking_microservices.transaction_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("alter table transactions drop constraint if exists transactions_status_check");
            jdbcTemplate.execute("""
                    alter table transactions add constraint transactions_status_check
                    check (status in (
                      'CREATED',
                      'VALIDATION_PENDING',
                      'FRAUD_REVIEW',
                      'BLOCK_MONEY',
                      'BLOCK_MONEY_FAILED',
                      'COMPLETED',
                      'CANCELLED',
                      'REVERSED',
                      'FAILED',
                      'DEPOSIT_FAILED',
                      'WITHDRAW_FAILED'
                    ))
                    """);
            log.info("Transaction status check constraint is up to date.");
        } catch (Exception e) {
            log.warn("Transaction status check constraint migration skipped: {}", e.getMessage());
        }
    }
}
