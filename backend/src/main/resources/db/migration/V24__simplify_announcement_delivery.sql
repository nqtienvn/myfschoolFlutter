DROP INDEX idx_announcement_reads_status ON announcement_reads;
DROP INDEX idx_announcement_reads_user_action ON announcement_reads;

ALTER TABLE announcement_reads
    DROP COLUMN acknowledged_at,
    DROP COLUMN reply_text,
    DROP COLUMN replied_at;

CREATE INDEX idx_announcement_reads_read_status
    ON announcement_reads(announcement_id, read_at);

ALTER TABLE announcements
    DROP FOREIGN KEY fk_ann_recipient_subject;

DROP INDEX idx_ann_recipient_subject ON announcements;

ALTER TABLE announcements
    DROP COLUMN requires_reply,
    DROP COLUMN teacher_audience,
    DROP COLUMN recipient_subject_id;
