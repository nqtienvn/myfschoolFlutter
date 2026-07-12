ALTER TABLE announcements
    ADD COLUMN recipient_scope VARCHAR(20) NOT NULL DEFAULT 'CLASSES' AFTER sender_type,
    ADD COLUMN teacher_audience VARCHAR(20) NULL AFTER recipient_scope,
    ADD COLUMN recipient_subject_id BIGINT NULL AFTER teacher_audience;

ALTER TABLE announcements
    ADD CONSTRAINT fk_ann_recipient_subject
        FOREIGN KEY (recipient_subject_id) REFERENCES subjects(id);

CREATE INDEX idx_ann_recipient_subject ON announcements(recipient_subject_id);

