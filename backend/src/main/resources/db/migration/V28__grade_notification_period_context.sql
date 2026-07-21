ALTER TABLE notifications
    ADD COLUMN academic_year_id BIGINT NULL AFTER related_type,
    ADD COLUMN semester_id BIGINT NULL AFTER academic_year_id;

CREATE INDEX idx_noti_grade_period
    ON notifications(academic_year_id, semester_id);
