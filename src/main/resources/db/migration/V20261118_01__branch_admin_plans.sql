-- Migración Flyway aditiva | catálogo administrativo de planes por sucursal.
-- Requiere esquema base GMS (branch, users y funciones de auditoría).
-- SCHEMA_DB se configura mediante spring.flyway.placeholders.schema_db.
-- No confundir con membership_plans: este catálogo no cobra ni crea suscripciones.
CREATE TABLE IF NOT EXISTS ${schema_db}.branch_admin_plans (
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_live_branch_admin_plan_name
    ON ${schema_db}.branch_admin_plans (branch_id, lower(btrim(name)))
    WHERE deleted_at IS NULL;

-- Los tres triggers pertenecen únicamente a la tabla nueva.
-- DROP IF EXISTS hace que Flyway tolere una instalación previa vía Liquibase.
DROP TRIGGER IF EXISTS trg_audit_metadata ON ${schema_db}.branch_admin_plans;
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
DROP TRIGGER IF EXISTS trg_audit_event ON ${schema_db}.branch_admin_plans;
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
DROP TRIGGER IF EXISTS trg_block_physical_delete ON ${schema_db}.branch_admin_plans;
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.branch_admin_plans
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
