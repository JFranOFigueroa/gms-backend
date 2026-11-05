# Provisionamiento MANUAL de usuarios y vínculos por SQL

**Estado:** procedimiento administrativo temporal confirmado. NO existe endpoint de registro global. Este documento es una guía y no inserta datos automáticamente.

## Seguridad antes de SQL

- Leer primero `README_MASTER_BACKEND.md`. Identifica el esquema efectivo (`schema_db` en `.env`); en ejemplos se usa **`test`**. Si utilizas otro esquema, reemplaza `test` en todas las consultas.
- Cada cuenta global debe insertarse con `status='active'` y `password_hash` BCrypt real **generado fuera de la consulta SQL**, por ejemplo mediante `BCryptPasswordEncoder(12)` en Java. No guardar contraseñas en texto plano, SQL, logs ni Git. El hash que se muestra como `HASH_BCRYPT_REAL` es solo un marcador, NO una contraseña válida.
- Generar UUID para cada entidad y reutilizar el mismo UUID al enlazar roles/membresías. Los usuarios pueden tener varios roles en gimnasios distintos.
- Ejecutar dentro de transacciones en un entorno controlado y auditar los cambios manuales. Permisos del motor SQL solo para operadores autorizados.

## 1. Usuario global activo

```sql
BEGIN;
INSERT INTO test.users (id, full_name, email, password_hash, status)
VALUES ('11111111-1111-4111-8111-111111111111', 'Nombre Ejemplo',
        'usuario@ejemplo.com', 'HASH_BCRYPT_REAL', 'active');
COMMIT;
```

Usa un UUID y hash real distintos para cada usuario. `users.email` es único globalmente.

## 2. Asignarlo al gimnasio como owner o trainer

Si el gimnasio ya existe (ejemplo de UUID `2222...`):

```sql
BEGIN;
INSERT INTO test.gym_staff_roles (gym_id, user_id, role, is_active)
VALUES ('22222222-2222-4222-8222-222222222222',
        '11111111-1111-4111-8111-111111111111', 'trainer', TRUE);
UPDATE test.users
SET default_gym_id = COALESCE(default_gym_id,
  '22222222-2222-4222-8222-222222222222')
WHERE id='11111111-1111-4111-8111-111111111111';
COMMIT;
```

`role` solo acepta `owner` y `trainer` en `gym_staff_roles`. El mismo usuario puede tener ambos roles cuando así lo indiquen las relaciones SQL. La matriz final de permisos aún está pendiente. También es posible asociar al usuario existente desde `POST /gyms/{gymId}/staff` con `userId` y `role` (sin crearlo).

## 3. Asociarlo como member

No existe un rol `member` en `gym_staff_roles`. El vínculo se guarda en `gym_members`:

```sql
BEGIN;
INSERT INTO test.gym_members (gym_id, user_id, member_code, is_active)
VALUES ('22222222-2222-4222-8222-222222222222',
        '11111111-1111-4111-8111-111111111111', 'M-001', TRUE);
UPDATE test.users
SET default_gym_id = COALESCE(default_gym_id,
  '22222222-2222-4222-8222-222222222222')
WHERE id='11111111-1111-4111-8111-111111111111';
COMMIT;
```

La alternativa desde API es `POST /gyms/{gymId}/members` con `userId`, `code`, etc., que **no crea** la cuenta global.

## 4. Administrador global

Solo asignar a un operador técnico autorizado:

```sql
INSERT INTO test.platform_admins (user_id)
VALUES ('11111111-1111-4111-8111-111111111111');
```

El `admin` global ve todos los gimnasios activos en su selector. La existencia de un usuario en `test.users` sin vínculos no le da acceso a ningún gimnasio.

## 5. Comprobaciones

```sql
SELECT user_id, gym_id, role, is_active
FROM test.v_user_roles
WHERE user_id='11111111-1111-4111-8111-111111111111';
```

Para login: probar `/api/v1/auth/login` con el email y contraseña cuyo hash se insertó; luego `/api/v1/auth/me`. Los gimnasios devueltos deben coincidir con relaciones activas, excepto admin global que ve todos los gimnasios activos. Ningún SELECT de este documento debe exponer `password_hash` en logs o respuestas API.
