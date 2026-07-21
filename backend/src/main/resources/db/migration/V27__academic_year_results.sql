CREATE TABLE academic_year_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    gpa DECIMAL(5,2) NULL,
    class_rank INT NULL,
    academic_ability VARCHAR(50) NOT NULL,
    conduct VARCHAR(50) NOT NULL,
    honor VARCHAR(80) NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_ayr_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_ayr_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_ayr_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT uq_ayr_student_year UNIQUE (student_id, academic_year_id)
);

CREATE INDEX idx_ayr_class_year_rank
    ON academic_year_results(class_id, academic_year_id, class_rank);

CREATE INDEX idx_ayr_year_published
    ON academic_year_results(academic_year_id, published_at);
