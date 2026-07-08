-- Fix message_receipts join columns to match Hibernate BIGINT parent PKs.
-- BaseEntity.id is Java Long, so Hibernate creates signed BIGINT.

-- Drop existing FKs first if they exist
ALTER TABLE message_receipts DROP FOREIGN KEY IF EXISTS fk_receipt_message;
ALTER TABLE message_receipts DROP FOREIGN KEY IF EXISTS fk_receipt_user;

-- Match referencing columns to parent PKs
ALTER TABLE message_receipts
    MODIFY COLUMN message_id BIGINT NOT NULL;

ALTER TABLE message_receipts
    MODIFY COLUMN user_id BIGINT NOT NULL;

-- Re-add foreign keys
ALTER TABLE message_receipts
    ADD CONSTRAINT fk_receipt_message
    FOREIGN KEY (message_id) REFERENCES messages (id);

ALTER TABLE message_receipts
    ADD CONSTRAINT fk_receipt_user
    FOREIGN KEY (user_id) REFERENCES users (id);
