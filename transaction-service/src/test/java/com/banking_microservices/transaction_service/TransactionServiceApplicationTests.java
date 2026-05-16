package com.banking_microservices.transaction_service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionServiceApplicationTests {

    @Test
    void applicationClassIsPresent() {
        assertThat(TransactionServiceApplication.class).isNotNull();
    }
}
