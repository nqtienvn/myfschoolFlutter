-- Fix message_receipts join columns to match BIGINT UNSIGNED parent PKs.
-- The manual schema defines messages.id and users.id as BIGINT UNSIGNED,
-- but message_receipts was created with signed BIGINT, causing FK incompatibility.

-- Drop existing FKs first if they exist
ALTER TABLE message_receipts DROP FOREIGN KEY IF EXISTS fk_receipt_message;
ALTER TABLE message_receipts DROP FOREIGN KEY IF EXISTS fk_receipt_user;

-- Widen referencing columns to match parent PKs
ALTER TABLE message_receipts
    MODIFY COLUMN message_id BIGINT UNSIGNED NOT NULL;

ALTER TABLE message_receipts
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL;

-- Re-add foreign keys
ALTER TABLE message_receipts
    ADD CONSTRAINT fk_receipt_message
    FOREIGN KEY (message_id) REFERENCES messages (id);

ALTER TABLE message_receipts
    ADD CONSTRAINT fk_receipt_user
    FOREIGN KEY (user_id) REFERENCES users (id);
