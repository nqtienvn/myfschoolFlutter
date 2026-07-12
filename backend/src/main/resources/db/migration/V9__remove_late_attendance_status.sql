-- LATE used to count as attended, so preserve historical attendance by
-- converting it to PRESENT before the application enum no longer accepts it.
UPDATE attendance
SET status = 'PRESENT'
WHERE status = 'LATE';

UPDATE attendance_details
SET status = 'PRESENT'
WHERE status = 'LATE';

ALTER TABLE attendance_sessions
    DROP COLUMN late;
