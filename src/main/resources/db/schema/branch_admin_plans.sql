--liquibase formatted sql

-- Catálogo administrativo de planes, independiente de membership_plans.
--changeset gms:branch-admin-plans-001 dbms:postgresql
CREATE TABLE ${schema_db}.branch_admin_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES ${schema_db}.branch(id) ON DELETE RESTRICT,
    name TEXT NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 120),
    description TEXT CHECK (description IS NULL OR length(description) <= 1000),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    duration_days INTEGER NOT NULL CHECK (duration_days BETWEEN 1 AND 3650),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);

-- El mismo gimnasio no puede tener dos planes vigentes con el mismo nombre.
-- Una eliminación lógica libera el nombre para un plan posterior.
CREATE UNIQUE INDEX ux_live_branch_admin_plan_name
    ON ${schema_db}.branch_admin_plans (branch_id, lower(btrim(name)))
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
