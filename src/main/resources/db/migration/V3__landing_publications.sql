-- Versión pública inmutable; publicación y booleano se modifican en la misma transacción.
CREATE TABLE ${schema_db}.landing_publications (
    gym_id UUID PRIMARY KEY REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    snapshot JSONB NOT NULL,
    draft_checksum CHAR(64) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);
