ALTER TABLE announcements MODIFY teacher_id BIGINT NULL;
ALTER TABLE announcements ADD COLUMN sender_user_id BIGINT NULL AFTER teacher_id;
ALTER TABLE announcements ADD COLUMN academic_year_id BIGINT NULL AFTER sender_user_id;
ALTER TABLE announcements ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE announcements ADD COLUMN rejection_reason VARCHAR(1000) NULL;
ALTER TABLE announcements ADD COLUMN sender_type VARCHAR(20) NOT NULL DEFAULT 'SUBJECT_TEACHER';
UPDATE announcements a JOIN teachers t ON t.id = a.teacher_id
SET a.sender_user_id = t.user_id;
UPDATE announcements a JOIN announcement_classes ac ON ac.announcement_id = a.id
JOIN classes c ON c.id = ac.class_id SET a.academic_year_id = c.academic_year_id
WHERE a.academic_year_id IS NULL;
ALTER TABLE announcements MODIFY sender_user_id BIGINT NOT NULL;
ALTER TABLE announcements MODIFY academic_year_id BIGINT NOT NULL;
ALTER TABLE announcements ADD CONSTRAINT fk_ann_sender FOREIGN KEY (sender_user_id) REFERENCES users(id);
ALTER TABLE announcements ADD CONSTRAINT fk_ann_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id);
CREATE INDEX idx_ann_year_status ON announcements(academic_year_id, approval_status, created_at);
