-- V6: código manual temporal, emitido solo al miembro autenticado y canjeado por kiosco autorizado.
-- No altera V1..V5 ni los contratos de Check-in y QR existentes.
CREATE TABLE ${schema_db}.checkin_access_codes (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 gym_id UUID NOT NULL,
 member_id UUID NOT NULL,
 code_hash VARCHAR(64) NOT NULL UNIQUE,
 issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 FOREIGN KEY (gym_id,member_id) REFERENCES ${schema_db}.gym_members(gym_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_checkin_manual_code_expiration CHECK (expires_at > issued_at)
);
CREATE INDEX ix_checkin_access_codes_member ON ${schema_db}.checkin_access_codes(gym_id,member_id,issued_at DESC)
 WHERE deleted_at IS NULL;
CREATE INDEX ix_checkin_access_codes_live ON ${schema_db}.checkin_access_codes(gym_id,expires_at)
 WHERE deleted_at IS NULL AND consumed_at IS NULL AND revoked_at IS NULL;
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
-- El método 'kiosk_code' y el índice único de accesos diarios ya existen desde V1.
