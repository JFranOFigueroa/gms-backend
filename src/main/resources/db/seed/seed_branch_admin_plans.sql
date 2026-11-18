--liquibase formatted sql

-- Semilla nueva. No modificar seed-001 ya registrado en DATABASECHANGELOG.
--changeset gms:seed-branch-admin-plans-001 dbms:postgresql
-- Plan exclusivamente demostrativo en la sucursal incluida en la semilla base.
-- No se agrega ningún plan a las sucursales reales, ni se sobreescriben cambios.
INSERT INTO ${schema_db}.branch_admin_plans (
    id, branch_id, name, description, price, duration_days, is_active
)
SELECT
    '77777777-7777-4777-8777-777777777781'::uuid,
    b.id,
    'Plan mensual demo',
    'Ejemplo administrativo para la sucursal Demo',
    399.00,
    30,
    TRUE
FROM ${schema_db}.branch b
WHERE b.id = '11111111-1111-1111-1111-111111111111'::uuid
  AND b.deleted_at IS NULL
ON CONFLICT DO NOTHING;
