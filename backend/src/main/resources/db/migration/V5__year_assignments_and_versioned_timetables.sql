-- Teaching assignments become year-scoped. Timetable versions own semester and effective dates.

DROP PROCEDURE IF EXISTS validate_year_assignment_migration;
DROP PROCEDURE IF EXISTS drop_foreign_key_if_exists;
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS create_index_if_missing;
DROP PROCEDURE IF EXISTS add_schedule_timetable_column_if_missing;

DELIMITER //
CREATE PROCEDURE validate_year_assignment_migration()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM teaching_assignments
        WHERE status = 'ACTIVE'
        GROUP BY class_id, subject_id
        HAVING COUNT(DISTINCT teacher_id) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot migrate: a class/subject has different active teachers across semesters';
    END IF;
END//

CREATE PROCEDURE drop_foreign_key_if_exists(IN table_name_param VARCHAR(64), IN key_name_param VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = table_name_param
          AND constraint_name = key_name_param AND constraint_type = 'FOREIGN KEY'
    ) THEN
        SET @drop_fk_sql = CONCAT('ALTER TABLE `', table_name_param, '` DROP FOREIGN KEY `', key_name_param, '`');
        PREPARE drop_fk_stmt FROM @drop_fk_sql;
        EXECUTE drop_fk_stmt;
        DEALLOCATE PREPARE drop_fk_stmt;
    END IF;
END//

CREATE PROCEDURE drop_index_if_exists(IN table_name_param VARCHAR(64), IN index_name_param VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = table_name_param AND index_name = index_name_param
    ) THEN
        SET @drop_index_sql = CONCAT('ALTER TABLE `', table_name_param, '` DROP INDEX `', index_name_param, '`');
        PREPARE drop_index_stmt FROM @drop_index_sql;
        EXECUTE drop_index_stmt;
        DEALLOCATE PREPARE drop_index_stmt;
    END IF;
END//

CREATE PROCEDURE create_index_if_missing(
    IN table_name_param VARCHAR(64), IN index_name_param VARCHAR(64), IN columns_param VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = table_name_param AND index_name = index_name_param
    ) THEN
        SET @create_index_sql = CONCAT('CREATE INDEX `', index_name_param, '` ON `', table_name_param, '` (', columns_param, ')');
        PREPARE create_index_stmt FROM @create_index_sql;
        EXECUTE create_index_stmt;
        DEALLOCATE PREPARE create_index_stmt;
    END IF;
END//

CREATE PROCEDURE add_schedule_timetable_column_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'schedules' AND column_name = 'timetable_id'
    ) THEN
        ALTER TABLE schedules ADD COLUMN timetable_id BIGINT NULL AFTER id;
    END IF;
END//
DELIMITER ;

CALL validate_year_assignment_migration();
DROP PROCEDURE validate_year_assignment_migration;

CREATE TABLE IF NOT EXISTS timetables (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_timetable_class_semester_version UNIQUE (class_id, semester_id, version),
    CONSTRAINT fk_tt_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_tt_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

INSERT IGNORE INTO timetables (
    class_id, semester_id, version, status, effective_from, effective_to, created_at, updated_at
)
SELECT DISTINCT ta.class_id, ta.semester_id, 1, 'ACTIVE', se.start_date, se.end_date, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM teaching_assignments ta
JOIN semesters se ON se.id = ta.semester_id;

CALL add_schedule_timetable_column_if_missing();

UPDATE schedules s
JOIN teaching_assignments ta ON ta.id = s.assignment_id
JOIN timetables tt ON tt.class_id = ta.class_id AND tt.semester_id = ta.semester_id AND tt.version = 1
SET s.timetable_id = tt.id;

-- Drop the old slot uniqueness before all semester assignments point to one canonical year assignment.
CALL create_index_if_missing('schedules', 'idx_schedule_assignment', '`assignment_id`');
CALL drop_index_if_exists('schedules', 'UKa0mera52v7xgpchwwnrxfosix');

CREATE TEMPORARY TABLE canonical_teaching_assignments AS
SELECT class_id, subject_id, MIN(id) AS canonical_id
FROM teaching_assignments
GROUP BY class_id, subject_id;

UPDATE schedules s
JOIN teaching_assignments ta ON ta.id = s.assignment_id
JOIN canonical_teaching_assignments c ON c.class_id = ta.class_id AND c.subject_id = ta.subject_id
SET s.assignment_id = c.canonical_id;

DELETE ta
FROM teaching_assignments ta
JOIN canonical_teaching_assignments c ON c.class_id = ta.class_id AND c.subject_id = ta.subject_id
WHERE ta.id <> c.canonical_id;

DROP TEMPORARY TABLE canonical_teaching_assignments;

CALL drop_foreign_key_if_exists('teaching_assignments', 'fk_ta_semester');
CALL create_index_if_missing('teaching_assignments', 'idx_ta_class', '`class_id`');
CALL drop_index_if_exists('teaching_assignments', 'UK9h7w183061rmd496cirjn2nlh');
CALL drop_index_if_exists('teaching_assignments', 'UKsngk7o0vf9ehukhi6e3lt0jjq');

ALTER TABLE teaching_assignments
    DROP COLUMN semester_id,
    ADD CONSTRAINT uq_ta_class_subject UNIQUE (class_id, subject_id);

ALTER TABLE schedules
    MODIFY timetable_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_schedule_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id),
    ADD CONSTRAINT uq_schedule_timetable_slot UNIQUE (timetable_id, day_of_week, period);

CREATE INDEX idx_timetable_effective
    ON timetables (class_id, semester_id, status, effective_from, effective_to);

DROP PROCEDURE drop_foreign_key_if_exists;
DROP PROCEDURE drop_index_if_exists;
DROP PROCEDURE create_index_if_missing;
DROP PROCEDURE add_schedule_timetable_column_if_missing;
