DROP TABLE IF EXISTS user_settings;

SET @department_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'teachers'
      AND column_name = 'department'
);

SET @drop_department_sql = IF(
    @department_exists > 0,
    'ALTER TABLE teachers DROP COLUMN department',
    'SELECT 1'
);

PREPARE drop_department_statement FROM @drop_department_sql;
EXECUTE drop_department_statement;
DEALLOCATE PREPARE drop_department_statement;
