-- Phase 1 consolidates attendance to one canonical session per class/date/shift.
-- Retain the newest legacy session and remove dependent details from older duplicates.
DELETE ad
FROM attendance_details ad
JOIN attendance_sessions older_session ON older_session.id = ad.session_id
JOIN attendance_sessions newer_session
  ON newer_session.class_id = older_session.class_id
 AND newer_session.date = older_session.date
 AND newer_session.shift = older_session.shift
 AND newer_session.id > older_session.id;

DELETE older_session
FROM attendance_sessions older_session
JOIN attendance_sessions newer_session
  ON newer_session.class_id = older_session.class_id
 AND newer_session.date = older_session.date
 AND newer_session.shift = older_session.shift
 AND newer_session.id > older_session.id;

ALTER TABLE attendance_sessions
    ADD CONSTRAINT uk_attendance_sessions_class_date_shift
    UNIQUE (class_id, date, shift);
