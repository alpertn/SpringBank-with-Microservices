package com.banking_microservices.money_service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMoneyServiceApplicationTests {

    @Test
    void applicationClassIsPresent() {
        assertThat(MoneyServiceApplication.class).isNotNull();
    }
}
