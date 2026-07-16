UPDATE subject_student_reviews
SET status = 'DRAFT', return_reason = NULL, submitted_at = NULL
WHERE status = 'RETURNED';

UPDATE student_events
SET status = 'SUBMITTED'
WHERE status = 'PUBLISHED';

ALTER TABLE student_events
    RENAME COLUMN published_at TO submitted_at;

ALTER TABLE semester_results
    ADD COLUMN suggested_honor VARCHAR(50) NULL,
    ADD COLUMN suggested_academic_ability VARCHAR(50) NULL,
    ADD COLUMN result_overridden BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN published_at DATETIME(6) NULL;

ALTER TABLE payment_configurations
    ADD COLUMN reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN reminder_interval_days INT NOT NULL DEFAULT 7;

ALTER TABLE tuition_bills
    ADD COLUMN last_reminder_at DATETIME(6) NULL;

UPDATE semester_results
SET suggested_honor = honor,
    suggested_academic_ability = academic_ability,
    result_overridden = CASE WHEN conduct_source = 'HOMEROOM' THEN TRUE ELSE FALSE END,
    conduct_source = CASE WHEN conduct_source = 'HOMEROOM' THEN 'ADMIN' ELSE conduct_source END;

UPDATE semester_results sr
JOIN student_periodic_reports spr
    ON spr.student_id = sr.student_id AND spr.semester_id = sr.semester_id
SET sr.published_at = COALESCE(spr.published_at, CURRENT_TIMESTAMP(6))
WHERE spr.status = 'PUBLISHED';

CREATE INDEX idx_sr_class_semester_published
    ON semester_results(class_id, semester_id, published_at);
