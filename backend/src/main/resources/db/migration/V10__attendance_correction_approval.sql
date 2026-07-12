CREATE TABLE attendance_correction_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    date DATE NOT NULL,
    shift VARCHAR(20) NOT NULL,
    proposed_entries LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_att_correction_day (class_id, date, shift, status),
    CONSTRAINT fk_att_correction_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_att_correction_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);
