-- À appliquer sur une base déjà migrée en phase 3, application arrêtée.
BEGIN;
-- Phase 4 : candidates for the bounded active-session query.
CREATE INDEX IF NOT EXISTS idx_sessions_active_client ON sessions(client_id,status,datetime) WHERE status IN ('PENDING','ACCEPTED','EN_ROUTE','ARRIVED','IN_PROGRESS');
CREATE INDEX IF NOT EXISTS idx_sessions_active_provider ON sessions(provider_id,status,datetime) WHERE status IN ('PENDING','ACCEPTED','EN_ROUTE','ARRIVED','IN_PROGRESS');

-- Durable SOS deliveries; never store an exception or SMTP response containing personal data.
CREATE TABLE IF NOT EXISTS sos_mail_queue(
 id BIGSERIAL PRIMARY KEY,alert_id BIGINT NOT NULL REFERENCES sos_alerts(id) ON DELETE CASCADE,
 recipient VARCHAR(255) NOT NULL,body TEXT NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','SENDING','SENT','FAILED','UNKNOWN')),
 attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(alert_id,recipient));
CREATE INDEX IF NOT EXISTS idx_sos_mail_pending ON sos_mail_queue(next_attempt_at,id) WHERE status='PENDING';
CREATE INDEX IF NOT EXISTS idx_sos_open ON sos_alerts(session_id,raised_by) WHERE status='OPEN';

COMMIT;
