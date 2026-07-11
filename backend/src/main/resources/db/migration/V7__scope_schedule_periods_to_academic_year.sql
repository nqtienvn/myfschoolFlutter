-- Period is now the source of truth for a timetable slot. The legacy numeric
-- period/shift columns remain populated for backward-compatible reads.
DELIMITER //
CREATE PROCEDURE add_schedule_period_ref_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'schedules' AND column_name = 'period_id'
    ) THEN
        ALTER TABLE schedules ADD COLUMN period_id BIGINT NULL AFTER period;
    END IF;
END//

CREATE PROCEDURE drop_schedule_slot_unique_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'schedules'
          AND index_name = 'uq_schedule_timetable_slot'
    ) THEN
        ALTER TABLE schedules DROP INDEX uq_schedule_timetable_slot;
    END IF;
END//

CREATE PROCEDURE add_schedule_period_fk_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'schedules'
          AND constraint_name = 'fk_sch_period' AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE schedules
            ADD CONSTRAINT fk_sch_period FOREIGN KEY (period_id) REFERENCES periods(id);
    END IF;
END//
DELIMITER ;

CALL add_schedule_period_ref_if_missing();

-- Prefer a period already configured for the timetable's academic year.
UPDATE schedules s
JOIN timetables tt ON tt.id = s.timetable_id
JOIN classes c ON c.id = tt.class_id
SET s.period_id = (
    SELECT MIN(p.id)
    FROM academic_year_periods ayp
    JOIN periods p ON p.id = ayp.period_id
    WHERE ayp.academic_year_id = c.academic_year_id
      AND p.display_order = s.period
)
WHERE s.period_id IS NULL;

-- Preserve legacy rows created before academic-year master data was applied.
UPDATE schedules s
SET s.period_id = (
    SELECT MIN(p.id) FROM periods p WHERE p.display_order = s.period
)
WHERE s.period_id IS NULL;

-- Any preserved legacy period becomes part of that year's explicit config.
INSERT IGNORE INTO academic_year_periods (academic_year_id, period_id, created_at, updated_at)
SELECT DISTINCT c.academic_year_id, s.period_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM schedules s
JOIN timetables tt ON tt.id = s.timetable_id
JOIN classes c ON c.id = tt.class_id
WHERE s.period_id IS NOT NULL;

INSERT IGNORE INTO academic_year_shifts (academic_year_id, shift_id, created_at, updated_at)
SELECT DISTINCT c.academic_year_id, p.shift_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM schedules s
JOIN timetables tt ON tt.id = s.timetable_id
JOIN classes c ON c.id = tt.class_id
JOIN periods p ON p.id = s.period_id
WHERE s.period_id IS NOT NULL;

CALL drop_schedule_slot_unique_if_exists();
ALTER TABLE schedules MODIFY period_id BIGINT NOT NULL;
CALL add_schedule_period_fk_if_missing();
ALTER TABLE schedules
    ADD CONSTRAINT uq_schedule_timetable_slot UNIQUE (timetable_id, day_of_week, period_id);

DROP PROCEDURE add_schedule_period_ref_if_missing;
DROP PROCEDURE drop_schedule_slot_unique_if_exists;
DROP PROCEDURE add_schedule_period_fk_if_missing;
