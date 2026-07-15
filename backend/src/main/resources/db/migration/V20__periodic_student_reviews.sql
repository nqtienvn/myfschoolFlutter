ALTER TABLE semester_results
    ADD COLUMN suggested_conduct VARCHAR(50) NULL AFTER conduct,
    ADD COLUMN conduct_source VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED' AFTER suggested_conduct;

UPDATE semester_results
SET suggested_conduct = conduct
WHERE suggested_conduct IS NULL;

CREATE TABLE subject_student_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    subject_teacher_id BIGINT NOT NULL,
    comment VARCHAR(2000) NULL,
    strengths VARCHAR(1000) NULL,
    improvements VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    return_reason VARCHAR(500) NULL,
    submitted_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_ssr_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_ssr_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_ssr_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_ssr_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_ssr_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_ssr_teacher FOREIGN KEY (subject_teacher_id) REFERENCES teachers(id),
    CONSTRAINT uq_ssr_student_subject_semester UNIQUE (student_id, subject_id, semester_id)
);

CREATE INDEX idx_ssr_class_semester_status
    ON subject_student_reviews(class_id, semester_id, status);

CREATE TABLE student_periodic_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    homeroom_teacher_id BIGINT NOT NULL,
    general_comment VARCHAR(2000) NULL,
    conduct VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_spr_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_spr_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_spr_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_spr_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_spr_teacher FOREIGN KEY (homeroom_teacher_id) REFERENCES teachers(id),
    CONSTRAINT uq_spr_student_semester UNIQUE (student_id, semester_id)
);

CREATE INDEX idx_spr_year_semester_status
    ON student_periodic_reports(academic_year_id, semester_id, status);

CREATE TABLE student_review_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_value_json LONGTEXT NULL,
    new_value_json LONGTEXT NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_sra_user FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_sra_entity ON student_review_audits(entity_type, entity_id, changed_at);
