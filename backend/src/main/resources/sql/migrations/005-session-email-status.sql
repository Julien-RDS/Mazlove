-- Additif : compatible avec les anciennes versions, qui ignorent ces colonnes.
BEGIN;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS client_email_unconfirmed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS provider_email_unconfirmed BOOLEAN NOT NULL DEFAULT false;
COMMIT;
