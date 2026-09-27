package com.banking_microservices.customer_service_command.dto.enums;

public enum RiskClass {
    RISK_1(1), RISK_2(2), RISK_3(3), RISK_4(4), RISK_5(5), RISK_6(6), RISK_7(7);

    private final int level;

    RiskClass(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
