package com.banking_microservices.fraud_service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FraudServiceApplicationTests {

    @Test
    void applicationClassIsPresent() {
        assertThat(FraudServiceApplication.class).isNotNull();
    }
}
