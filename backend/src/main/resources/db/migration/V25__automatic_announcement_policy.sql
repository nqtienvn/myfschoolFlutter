DELETE FROM notifications
WHERE related_type IN ('ANNOUNCEMENT', 'ANNOUNCEMENT_APPROVAL');

DELETE FROM announcements;

DROP INDEX idx_ann_year_status ON announcements;

ALTER TABLE announcements
    CHANGE COLUMN approval_status delivery_status VARCHAR(24) NOT NULL,
    DROP COLUMN rejection_reason,
    ADD COLUMN system_rejection_message VARCHAR(500) NULL AFTER delivery_status,
    ADD COLUMN retry_of_announcement_id BIGINT NULL AFTER system_rejection_message,
    ADD CONSTRAINT fk_ann_retry FOREIGN KEY (retry_of_announcement_id) REFERENCES announcements(id) ON DELETE SET NULL;

CREATE INDEX idx_ann_year_delivery_created
    ON announcements(academic_year_id, delivery_status, created_at, id);

CREATE INDEX idx_ann_retry ON announcements(retry_of_announcement_id);

CREATE TABLE announcement_policy_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rejection_message VARCHAR(500) NOT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_announcement_policy_year UNIQUE (academic_year_id),
    CONSTRAINT fk_announcement_policy_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_policy_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE announcement_content_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    phrase VARCHAR(250) NOT NULL,
    normalized_phrase VARCHAR(250) NOT NULL,
    match_scope VARCHAR(12) NOT NULL,
    match_type VARCHAR(12) NOT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_announcement_rule UNIQUE (academic_year_id, normalized_phrase, match_scope, match_type),
    CONSTRAINT fk_announcement_rule_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_rule_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_announcement_rule_year ON announcement_content_rules(academic_year_id);

CREATE TABLE announcement_policy_violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_id BIGINT NOT NULL,
    rule_id BIGINT NULL,
    matched_field VARCHAR(12) NOT NULL,
    matched_phrase VARCHAR(250) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_announcement_violation_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_violation_rule FOREIGN KEY (rule_id) REFERENCES announcement_content_rules(id) ON DELETE SET NULL
);

CREATE INDEX idx_announcement_violation_announcement
    ON announcement_policy_violations(announcement_id);
