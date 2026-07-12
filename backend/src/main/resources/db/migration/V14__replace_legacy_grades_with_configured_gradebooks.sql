DROP TABLE IF EXISTS grades;

CREATE TABLE grade_config_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6), updated_at DATETIME(6),
    CONSTRAINT uq_grade_template_name_version UNIQUE (name, version)
);

CREATE TABLE grade_config_template_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    weight INT NOT NULL,
    quantity INT NOT NULL,
    entry_role VARCHAR(30) NOT NULL,
    assessment_type VARCHAR(30) NOT NULL DEFAULT 'SCORE',
    required_entry BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL,
    created_at DATETIME(6), updated_at DATETIME(6),
    CONSTRAINT fk_gcti_template FOREIGN KEY (template_id) REFERENCES grade_config_templates(id),
    CONSTRAINT uq_gcti_code UNIQUE (template_id, code),
    CONSTRAINT ck_gcti_weight CHECK (weight > 0),
    CONSTRAINT ck_gcti_quantity CHECK (quantity > 0)
);

CREATE TABLE academic_year_grade_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    source_template_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALIDATED',
    created_at DATETIME(6), updated_at DATETIME(6),
    CONSTRAINT fk_aygc_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_aygc_template FOREIGN KEY (source_template_id) REFERENCES grade_config_templates(id),
    CONSTRAINT uq_aygc_year UNIQUE (academic_year_id)
);

CREATE TABLE academic_year_grade_config_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    weight INT NOT NULL,
    quantity INT NOT NULL,
    entry_role VARCHAR(30) NOT NULL,
    assessment_type VARCHAR(30) NOT NULL DEFAULT 'SCORE',
    required_entry BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL,
    created_at DATETIME(6), updated_at DATETIME(6),
    CONSTRAINT fk_aygci_config FOREIGN KEY (config_id) REFERENCES academic_year_grade_configs(id),
    CONSTRAINT uq_aygci_code UNIQUE (config_id, code)
);

ALTER TABLE grade_items
    ADD COLUMN config_item_id BIGINT NULL,
    ADD COLUMN code VARCHAR(50) NULL,
    ADD COLUMN entry_role VARCHAR(30) NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN assessment_type VARCHAR(30) NOT NULL DEFAULT 'SCORE',
    ADD COLUMN required_entry BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT fk_gi_config_item FOREIGN KEY (config_item_id) REFERENCES academic_year_grade_config_items(id);

ALTER TABLE grade_books
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE student_scores
    ADD COLUMN published_at DATETIME(6) NULL,
    ADD COLUMN entered_by BIGINT NULL,
    ADD CONSTRAINT fk_ss_entered_by FOREIGN KEY (entered_by) REFERENCES users(id);

CREATE TABLE student_score_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_score_id BIGINT NOT NULL,
    old_score DECIMAL(5,2) NULL,
    new_score DECIMAL(5,2) NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    changed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_ssa_score FOREIGN KEY (student_score_id) REFERENCES student_scores(id),
    CONSTRAINT fk_ssa_user FOREIGN KEY (changed_by) REFERENCES users(id)
);
