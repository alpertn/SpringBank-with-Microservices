ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS national_id_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS national_id_masked VARCHAR(20),
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_ACTIVATION',
    ADD COLUMN IF NOT EXISTS marital_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS risk_class VARCHAR(20) NOT NULL DEFAULT 'RISK_1',
    ADD COLUMN IF NOT EXISTS segment VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS segment_score INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS political_exposure_status VARCHAR(40) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS employment_category VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS address_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_raw VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS address_country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_country_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS address_province VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_district VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_neighborhood VARCHAR(150),
    ADD COLUMN IF NOT EXISTS address_road VARCHAR(200),
    ADD COLUMN IF NOT EXISTS address_building VARCHAR(150),
    ADD COLUMN IF NOT EXISTS address_building_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_entrance VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_floor VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_unit VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS address_parse_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address_parser VARCHAR(50),
    ADD COLUMN IF NOT EXISTS address_parser_version VARCHAR(80);

UPDATE customers SET national_id_hash = 'legacy:' || id, national_id_masked = '*******LEGACY'
WHERE national_id_hash IS NULL;
ALTER TABLE customers ALTER COLUMN national_id_hash SET NOT NULL;
ALTER TABLE customers ALTER COLUMN national_id_masked SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_national_id_hash ON customers (national_id_hash);
CREATE INDEX IF NOT EXISTS idx_customers_segment ON customers (segment);

ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_segment_score;
ALTER TABLE customers ADD CONSTRAINT chk_customers_segment_score CHECK (segment_score BETWEEN 1 AND 10);
ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_risk_score;
ALTER TABLE customers ADD CONSTRAINT chk_customers_risk_score CHECK (risk_score BETWEEN 0 AND 100);
