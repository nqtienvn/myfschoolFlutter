ALTER TABLE leave_requests
    ADD COLUMN academic_year_id BIGINT NULL AFTER class_id;

UPDATE leave_requests lr
JOIN classes c ON c.id = lr.class_id
SET lr.academic_year_id = c.academic_year_id
WHERE lr.academic_year_id IS NULL;

ALTER TABLE leave_requests
    MODIFY academic_year_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_lr_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD INDEX idx_lr_academic_year_status (academic_year_id, status);

ALTER TABLE notifications
    ADD COLUMN related_id BIGINT NULL AFTER tag,
    ADD COLUMN related_type VARCHAR(50) NULL AFTER related_id,
    ADD INDEX idx_notifications_related (related_type, related_id);
