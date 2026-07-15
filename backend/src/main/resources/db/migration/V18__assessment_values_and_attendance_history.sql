ALTER TABLE student_score_audits
    ADD COLUMN old_comment VARCHAR(255) NULL,
    ADD COLUMN new_comment VARCHAR(255) NULL,
    ADD COLUMN old_is_graded BOOLEAN NULL,
    ADD COLUMN new_is_graded BOOLEAN NULL;

ALTER TABLE attendance_correction_requests
    ADD COLUMN original_entries LONGTEXT NULL,
    ADD COLUMN reason VARCHAR(500) NULL,
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD CONSTRAINT fk_att_correction_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id);

UPDATE attendance_correction_requests
SET reason = 'Yêu cầu được tạo trước khi hệ thống bắt buộc nhập lý do'
WHERE reason IS NULL OR TRIM(reason) = '';

ALTER TABLE attendance_correction_requests
    MODIFY COLUMN reason VARCHAR(500) NOT NULL;
