-- Requests with the same idempotency key in the same gym/actor/operation are serialized.
-- No existing tables, columns, endpoint payloads or historical migrations are changed.
CREATE TABLE ${schema_db}.request_idempotency (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
 actor_id UUID NOT NULL REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
 operation VARCHAR(32) NOT NULL CHECK (operation IN ('payment','sale')),
 idempotency_key VARCHAR(128) NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 resource_id UUID,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 CONSTRAINT ux_request_idempotency UNIQUE (gym_id,actor_id,operation,idempotency_key)
);
CREATE INDEX ix_request_idempotency_created ON ${schema_db}.request_idempotency(created_at);
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.request_idempotency
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.request_idempotency
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.request_idempotency
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
