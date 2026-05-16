package com.banking_microservices.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayApplicationTests {

    @Test
    void applicationClassIsPresent() {
        assertThat(GatewayApplication.class).isNotNull();
    }
}
