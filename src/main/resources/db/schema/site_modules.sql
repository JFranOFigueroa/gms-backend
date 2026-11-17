--liquibase formatted sql

-- Changeset aditivo. El baseline schema-001 ya fue aplicado y no se modifica.
-- Los módulos globales viven en site_settings para que todas las sucursales
-- compartan la misma configuración cuando modules_global = TRUE.
--changeset gms:site-modules-001 dbms:postgresql
ALTER TABLE ${schema_db}.site_settings
    ADD COLUMN IF NOT EXISTS modules_global BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS global_modules JSONB NOT NULL
        DEFAULT '{"calendar":false,"routines":true,"checkin":true,"pos":true}'::jsonb;
