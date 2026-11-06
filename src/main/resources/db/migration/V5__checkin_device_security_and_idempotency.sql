-- V5: ampliación no destructiva de Check-in. V1..V4 permanecen intactas.
ALTER TABLE ${schema_db}.checkin_devices
  ADD COLUMN activation_expires_at TIMESTAMPTZ,
  ADD COLUMN activated_at TIMESTAMPTZ;

ALTER TABLE ${schema_db}.checkin_events
  ADD COLUMN idempotency_key_hash VARCHAR(64),
  ADD COLUMN request_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX ux_live_checkin_idempotency
 ON ${schema_db}.checkin_events(gym_id,idempotency_key_hash)
 WHERE idempotency_key_hash IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE ${schema_db}.checkin_device_sessions (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 gym_id UUID NOT NULL,
 device_id UUID NOT NULL,
 session_token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 last_seen_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID,
 FOREIGN KEY (gym_id,device_id) REFERENCES ${schema_db}.checkin_devices(gym_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_kiosk_session_expiration CHECK (expires_at > created_at)
);
CREATE INDEX ix_checkin_device_sessions_live ON ${schema_db}.checkin_device_sessions(device_id,expires_at)
 WHERE deleted_at IS NULL AND revoked_at IS NULL;

-- Registrar nueva tabla en el mecanismo transversal de auditoría y bloqueo de DELETE.
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();

-- Una fecha de vencimiento ausente NO da acceso, incluso si existe fecha de pago.
-- Mantener los mismos nombres de campos y prioridades para el módulo manual existente.
CREATE OR REPLACE VIEW ${schema_db}.v_member_access AS
 SELECT m.id AS member_id,m.gym_id,m.user_id,m.member_code,s.next_due_date,
 CASE WHEN g.status<>'active' THEN 'gym_inactive'
      WHEN u.status<>'active' THEN 'user_inactive'
      WHEN NOT m.is_active THEN 'member_inactive'
      WHEN s.id IS NULL THEN 'no_active_subscription'
      WHEN s.last_payment_date IS NULL THEN 'no_payment'
      WHEN s.next_due_date IS NULL THEN 'unknown_validity'
      WHEN s.next_due_date<(now() AT TIME ZONE g.timezone)::date THEN 'expired'
      ELSE 'allowed' END AS access_status
 FROM ${schema_db}.gym_members m
 JOIN ${schema_db}.gym g ON g.id=m.gym_id AND g.deleted_at IS NULL
 JOIN ${schema_db}.users u ON u.id=m.user_id AND u.deleted_at IS NULL
 LEFT JOIN ${schema_db}.member_subscriptions s
   ON s.gym_id=m.gym_id AND s.member_id=m.id AND s.status='active' AND s.deleted_at IS NULL
 WHERE m.deleted_at IS NULL;

-- Límite de abuso compartido entre instancias para intentos de activación.
CREATE TABLE ${schema_db}.checkin_activation_attempts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 client_hash VARCHAR(64) NOT NULL,
 tried_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID
);
CREATE INDEX ix_checkin_activation_attempts_client ON ${schema_db}.checkin_activation_attempts(client_hash,tried_at)
 WHERE deleted_at IS NULL;
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
