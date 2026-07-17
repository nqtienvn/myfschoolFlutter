ALTER TABLE semester_results
    MODIFY COLUMN conduct_source VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED';

UPDATE semester_results
SET conduct_source = 'ADMIN',
    result_overridden = TRUE
WHERE conduct_source = 'HOMEROOM';
