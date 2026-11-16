# Seguridad y base de datos

## Esquema

- `schema_db` (alias `SCHEMA_DB`) configura el esquema PostgreSQL.
- Solo se acepta un identificador validado `[a-z_][a-z0-9_]{0,62}`.
- El esquema no se acepta desde rutas HTTP.

## Identidad

- `users` es global.
- `platform_admins` identifica administradores globales.
- `branch_staff_roles` contiene `owner`/`trainer` por sucursal.
- `branch_members` determina la membresía por sucursal.
- Una persona puede tener roles distintos en sucursales distintas.

## Autenticación

- JWT HS256 con `JWT_SECRET` de al menos 32 bytes.
- Access token de vida corta; refresh token rotativo.
- `/auth/me` reconstruye la misma sesión que `/auth/login`.
- La selección de sucursal ocurre después de validar credenciales.
- Un `platformAdmin` puede autenticarse sin sucursales.
- Cada endpoint operativo valida el `branchId` y el rol en servidor.

## Autorización

- `platformAdmin`: acceso global.
- `owner`: administra su sucursal y puede editar el sitio.
- `trainer`: acceso operativo limitado.
- `member`: consulta sus datos, rutinas y check-in.
- La matriz fina de permisos sigue pendiente de aprobación.

## Aislamiento

- `BaseCRUD` exige `branchId` en entidades tenant.
- Las consultas operativas filtran por `branch_id`.
- No se aceptan IDs de otra sucursal sin validación.
- Las pruebas de integración cubren check-in y aislamiento básico.

## Privacidad

- No se emite `password_hash`.
- Las fotos de miembros se sirven con Bearer y `Cache-Control: no-store`.
- `site_media_assets` solo contiene imágenes públicas del sitio.
- La landing pública no expone usuarios, pagos, check-ins ni fotos privadas.

## Archivos

- `LocalImageStorage` valida cabeceras PNG/JPG/WebP, tamaño máximo 8 MB y rutas.
- El scope `site` se usa para multimedia del sitio.
- El scope `member-photos/{branchId}` se usa para fotos privadas.
- Los archivos se guardan bajo `GMS_FILES_ROOT`.

## Base de datos

- Baseline: `src/main/resources/db/schema/schema.sql`.
- Seed: `src/main/resources/db/seed/seed.sql`.
- Idempotencia: `src/main/resources/db/schema/idempotency_operations.sql`.
- Changelog: `src/main/resources/db/changelog-master.yaml`.
- `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway deshabilitado por defecto.
- El baseline es inmutable una vez aplicado; los cambios nuevos van en changesets adicionales.

## CORS y secretos

- Configurar `FRONTEND_ORIGIN` explícito.
- Usar HTTPS fuera de local.
- No versionar `.env` ni credenciales.
- `BOOTSTRAP_ADMIN_*` es solo para la creación inicial del administrador.
