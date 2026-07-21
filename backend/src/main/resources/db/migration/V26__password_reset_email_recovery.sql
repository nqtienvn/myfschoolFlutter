ALTER TABLE users
    ADD COLUMN email_verified_at DATETIME(6) NULL AFTER email,
    ADD COLUMN credentials_updated_at DATETIME(6) NULL AFTER password;

UPDATE users
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    requested_ip VARCHAR(45) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    active_user_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN used_at IS NULL THEN user_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT uk_password_reset_active_user UNIQUE (active_user_id),
    INDEX idx_password_reset_user_created (user_id, created_at),
    INDEX idx_password_reset_ip_created (requested_ip, created_at)
);

CREATE TABLE password_reset_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    phone_hash CHAR(64) NOT NULL,
    requested_ip VARCHAR(45) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_password_reset_attempt_phone (phone_hash, created_at),
    INDEX idx_password_reset_attempt_user (user_id, created_at),
    INDEX idx_password_reset_attempt_ip (requested_ip, created_at)
);
