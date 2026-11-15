# Estado de integración backend

## Arquitectura vigente

- **Sitio global**: `site_settings`, `site_landing_blocks`, `site_media_assets`.
- **Sucursal operativa**: `branch`, `branch_settings`, `branch_modules`, `branch_members`, `branch_staff_roles`, `branch_classes`, etc.
- **Landing única**: `GET /api/v1/public/site`.
- **Kiosco**: `?branch=<code>` + `POST /api/v1/kiosk/activate`.

No existen landings por sucursal, `landing_publications`, `media_assets` ni rutas `/g/{slug}`.

## Módulos implementados

| Área | Estado |
|---|---|
| Autenticación JWT + refresh | Implementado |
| Sitio: settings, tema, landing, media | Implementado |
| Sucursales | Implementado |
| Miembros y fotos privadas | Implementado |
| Staff | Implementado |
| Planes y pagos manuales | Implementado |
| Calendario, plantillas y reservas | Implementado |
| Rutinas | Implementado |
| POS e inventario | Implementado |
| Check-in QR, códigos y dispositivos | Implementado |
| Dashboard agregado | Implementado |
| Auditoría transversal | Implementado por triggers |

## Contratos clave

- Login: `POST /api/v1/auth/login`.
- Sesión: `GET /api/v1/auth/me`.
- Refresh: `POST /api/v1/auth/refresh`.
- Sitio público: `GET /api/v1/public/site`.
- Settings del sitio: `GET|PATCH /api/v1/site/settings`.
- Landing: CRUD `/api/v1/site/landing-blocks`.
- Media del sitio: CRUD `/api/v1/site/media` + `POST /upload`.
- Sucursales: CRUD `/api/v1/branches` (solo `platformAdmin`).
- Operación: `/api/v1/branches/{branchId}/...`.
- Fotos privadas: `/api/v1/branches/{branchId}/members/{memberId}/photo`.
- Kiosco: `/api/v1/kiosk/...`.

## Base de datos

- Baseline: `src/main/resources/db/schema/schema.sql`.
- Seed idempotente: `src/main/resources/db/seed/seed.sql`.
- Idempotencia: `src/main/resources/db/schema/idempotency_operations.sql`.
- Changelog: `src/main/resources/db/changelog-master.yaml`.
- Flyway deshabilitado por defecto; el esquema se prepara con Liquibase.

## Validación

- `mvn test`: 75 tests, 0 fallos, 17 omitidos.
- `KioskPostgresIntegrationTest`: 21 tests, 0 fallos, 1 omitido.
- Esquema, seed e idempotencia aplicados en PostgreSQL 15 limpio.
- Arranque local con `ddl-auto=validate` y `AdminBootstrap` verificado.
- Endpoints de sitio, media, sucursales, miembros y fotos verificados por HTTP.

## Pendiente

- QA con navegador real del flujo completo de landing y kiosco.
- Validación de despliegue en WildFly.
- Matriz definitiva de permisos por módulo.
- Sustitución de `alert()`/`confirm()` nativos en el frontend.
