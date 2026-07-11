DROP PROCEDURE IF EXISTS add_scheduled_timetable_index;

DELIMITER //
CREATE PROCEDURE add_scheduled_timetable_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'timetables'
          AND index_name = 'idx_timetable_scheduled_publish'
    ) THEN
        CREATE INDEX idx_timetable_scheduled_publish
            ON timetables (status, effective_from);
    END IF;
END//
DELIMITER ;

CALL add_scheduled_timetable_index();
DROP PROCEDURE add_scheduled_timetable_index;
