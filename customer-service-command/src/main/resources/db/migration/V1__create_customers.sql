CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    keycloak_id UUID NOT NULL UNIQUE,
    realm VARCHAR(40) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_number VARCHAR(40),
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    user_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    birthdate TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    surname VARCHAR(100) NOT NULL,
    sex VARCHAR(40),
    risk_score INTEGER NOT NULL DEFAULT 0,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_method VARCHAR(40) NOT NULL,
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'tr',
    special_customer BOOLEAN NOT NULL DEFAULT FALSE,
    special_customer_score INTEGER NOT NULL DEFAULT 0,
    kyc_status VARCHAR(40) NOT NULL,
    nationality_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_customers_keycloak_id ON customers (keycloak_id);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers (status);
CREATE INDEX IF NOT EXISTS idx_customers_kyc_status ON customers (kyc_status);
CREATE INDEX IF NOT EXISTS idx_customers_deleted ON customers (is_deleted);
