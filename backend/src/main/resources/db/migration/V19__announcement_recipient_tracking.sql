ALTER TABLE announcement_reads
    ADD COLUMN acknowledged_at DATETIME NULL AFTER read_at,
    ADD COLUMN reply_text VARCHAR(1000) NULL AFTER acknowledged_at,
    ADD COLUMN replied_at DATETIME NULL AFTER reply_text,
    ADD COLUMN recipient_role VARCHAR(20) NULL AFTER user_id,
    ADD COLUMN user_name VARCHAR(255) NULL AFTER recipient_role,
    ADD COLUMN student_names TEXT NULL AFTER user_name,
    ADD COLUMN class_names TEXT NULL AFTER student_names,
    ADD COLUMN class_ids TEXT NULL AFTER class_names;

UPDATE announcement_reads ar
JOIN users u ON u.id = ar.user_id
SET ar.recipient_role = u.role,
    ar.user_name = u.name
WHERE ar.recipient_role IS NULL OR ar.user_name IS NULL;

CREATE INDEX idx_announcement_reads_status
    ON announcement_reads(announcement_id, read_at, acknowledged_at, replied_at);

CREATE INDEX idx_announcement_reads_user_action
    ON announcement_reads(user_id, acknowledged_at, replied_at);
