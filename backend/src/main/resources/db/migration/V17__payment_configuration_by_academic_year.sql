CREATE TABLE payment_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    bank_code VARCHAR(30) NULL,
    bank_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_holder VARCHAR(150) NOT NULL,
    branch VARCHAR(150) NULL,
    transfer_content_template VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_payment_configurations_academic_year UNIQUE (academic_year_id),
    CONSTRAINT fk_payment_configurations_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id)
);
