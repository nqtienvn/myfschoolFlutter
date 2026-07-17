UPDATE semester_results
SET conduct_source = 'ADMIN',
    result_overridden = TRUE
WHERE conduct_source = 'HOMEROOM';
