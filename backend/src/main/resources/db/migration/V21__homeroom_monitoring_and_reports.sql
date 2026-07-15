CREATE TABLE student_risk_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    min_gpa DECIMAL(4,2) NULL,
    min_attendance_rate DECIMAL(5,2) NULL,
    max_unexcused_absences INT NULL,
    conduct_risk_values VARCHAR(200) NULL,
    include_overdue_tuition BOOLEAN NOT NULL DEFAULT FALSE,
    overdue_tuition_days INT NOT NULL DEFAULT 0,
    gpa_severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    attendance_severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    absence_severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    conduct_severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    tuition_severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_src_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT uq_src_year UNIQUE (academic_year_id)
);

CREATE TABLE student_risk_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    risk_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    metric_value VARCHAR(100) NULL,
    threshold_value VARCHAR(100) NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    detected_at DATETIME(6) NOT NULL,
    acknowledged_by BIGINT NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME(6) NULL,
    source_snapshot_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_srf_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_srf_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_srf_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_srf_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_srf_ack_user FOREIGN KEY (acknowledged_by) REFERENCES users(id),
    CONSTRAINT fk_srf_resolved_user FOREIGN KEY (resolved_by) REFERENCES users(id),
    CONSTRAINT uq_srf_scope UNIQUE (academic_year_id, semester_id, class_id, student_id, risk_type)
);

CREATE INDEX idx_srf_class_status ON student_risk_flags(class_id, semester_id, status);

CREATE TABLE parent_contact_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    contact_type VARCHAR(20) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    result VARCHAR(1000) NULL,
    contacted_at DATETIME(6) NOT NULL,
    next_action_at DATETIME(6) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_pcl_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_pcl_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_pcl_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_pcl_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_pcl_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_pcl_student_period ON parent_contact_logs(student_id, semester_id, contacted_at);

CREATE TABLE parent_meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NULL,
    starts_at DATETIME(6) NOT NULL,
    location VARCHAR(300) NULL,
    agenda VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_pm_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_pm_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_pm_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_pm_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_pm_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE parent_meeting_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    guardian_id BIGINT NOT NULL,
    response VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attendance VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    responded_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_pmp_meeting FOREIGN KEY (meeting_id) REFERENCES parent_meetings(id),
    CONSTRAINT fk_pmp_guardian FOREIGN KEY (guardian_id) REFERENCES parents(id),
    CONSTRAINT uq_pmp_guardian UNIQUE (meeting_id, guardian_id)
);

CREATE INDEX idx_pm_class_period ON parent_meetings(class_id, semester_id, starts_at);

CREATE TABLE student_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    category VARCHAR(100) NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    event_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_se_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_se_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_se_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_se_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_se_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_se_student_period ON student_events(student_id, semester_id, event_date);
