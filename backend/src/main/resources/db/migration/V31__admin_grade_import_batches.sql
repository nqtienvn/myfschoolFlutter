CREATE TABLE grade_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    config_item_id BIGINT NOT NULL,
    item_occurrence INT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    total_rows INT NOT NULL DEFAULT 0,
    updated_scores INT NOT NULL DEFAULT 0,
    imported_by BIGINT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_gib_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_gib_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_gib_config_item FOREIGN KEY (config_item_id) REFERENCES academic_year_grade_config_items(id),
    CONSTRAINT fk_gib_imported_by FOREIGN KEY (imported_by) REFERENCES users(id),
    INDEX idx_gib_semester_item (semester_id, config_item_id, item_occurrence, created_at)
);

CREATE TABLE grade_import_rows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    source_order INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_gir_batch FOREIGN KEY (batch_id) REFERENCES grade_import_batches(id),
    CONSTRAINT fk_gir_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_gir_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT uq_gir_batch_student UNIQUE (batch_id, student_id),
    CONSTRAINT uq_gir_batch_order UNIQUE (batch_id, source_order),
    INDEX idx_gir_batch_class_order (batch_id, class_id, source_order)
);
