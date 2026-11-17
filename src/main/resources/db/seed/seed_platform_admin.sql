--liquibase formatted sql

-- Cuenta inicial del administrador del sistema.
-- La contraseña queda en este changeset por decisión explícita del proyecto;
-- se almacena como hash BCrypt en la base y debe cambiarse fuera de la UI.
--changeset gms:seed-platform-admin-001 dbms:postgresql

INSERT INTO ${schema_db}.users (
    id, full_name, email, password_hash, status
) VALUES (
    '99999999-9999-9999-9999-999999999999',
    'Administrador del sistema',
    'admin@gms.local',
    crypt('Admin123456789!', gen_salt('bf', 12)),
    'active'
) ON CONFLICT (email) DO UPDATE
SET password_hash = COALESCE(users.password_hash, EXCLUDED.password_hash),
    status = CASE WHEN users.status = 'invited' THEN 'active' ELSE users.status END
WHERE users.password_hash IS NULL OR users.status = 'invited';

-- El rol global se enlaza por correo para que el changeset sea idempotente
-- incluso si la cuenta ya existía con otro UUID.
INSERT INTO ${schema_db}.platform_admins (user_id, assigned_by)
SELECT u.id, u.id
FROM ${schema_db}.users u
WHERE lower(u.email) = 'admin@gms.local' AND u.deleted_at IS NULL
ON CONFLICT (user_id) DO UPDATE
SET deleted_at = NULL, deleted_by = NULL, assigned_at = now();

-- Sucursal Demo es la sucursal inicial del seed. No se asigna owner aquí:
-- el administrador global ya opera sobre todas las sucursales.
UPDATE ${schema_db}.users
SET default_branch_id = '11111111-1111-1111-1111-111111111111'
WHERE lower(email) = 'admin@gms.local'
  AND deleted_at IS NULL
  AND default_branch_id IS NULL
  AND EXISTS (
      SELECT 1 FROM ${schema_db}.branch
      WHERE id = '11111111-1111-1111-1111-111111111111'
        AND deleted_at IS NULL
  );
