> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

## 2026-11-03 — Integración parcial de especificación de endpoints y landing pública

- Nueva Flyway V3 para snapshot/versionado de publicación; publicación/despublicación del owner y lectura anónima filtrada con URL por slug.
- Auto slug desde nombre en POST `/gyms`, PATCH acotado de slug para owner, validación y conflicto 409.
- GET propio de miembro, reservas, cupos, accesos; filtros de calendario, dashboard SQL agregado; corrección de user_id en detalle de miembros.
- CRUD de usuarios + registro guiado de miembros/staff detrás de `USER_API_ENABLED=false` por compatibilidad con alta SQL manual vigente.
- No implementados upload de binarios, QR de kiosco ni guest-passes (requieren decisiones de infraestructura/políticas).
- No se alteraron V1/V2. Sin `mvn` ni `pom.xml` en entrada: parseo Java 152 archivos sin errores y prueba aislada de slug; **no** hay validación JDBC/Flyway/HTTP.
- Inventario y procedimientos detallados: `docs/INTEGRACION_BACKEND_ESTADO.md`.

# Registro de cambios

## 2026-11-03 — Corrección de compilación informada desde Windows

- `StaffRepository`: se tipan explícitamente como `ResultSetExtractor<List<Map<String,Object>>>` las dos consultas que agrupan los roles, resolviendo la ambigüedad de sobrecargas de `JdbcTemplate.query`. Se conserva el agrupamiento y el contrato existente.
- `IIdentityRepository`: incorpora `insertPlatformAdmin(UUID)`, que ya requería `AdminBootstrap`; `IdentityRepository` implementa la inserción en `platform_admins` dentro de la transacción de bootstrap.
- No se cambian rutas, DTO, permisos, migraciones ni reglas de negocio.
- Pendiente de validar con `mvn clean test` y PostgreSQL en el equipo local.


## 2026-11-03 — Revisión backend alineada con decisiones del usuario

- **Fuente utilizada:** último `src.zip` de backend proporcionado por usuario, fusionado con los archivos de proyecto y docs del ZIP backend anterior. Se verificaron rutas; no se detectaron `{{gymId}}`.
- **Identidad:** DTO tipados `AuthUser`, `GymAccess`, `AuthSession`, `LoginResponse`. `login` y `me` utilizan la misma construcción de identidad y gimnasios. `gymsForUser` obtiene roles efectivos del gimnasio en una consulta agrupada, con vista admin global. Selector se implementará en Vue después de autenticar, dentro del flujo de login.
- **Miembros:** se elimina del servicio la creación automática de usuario invitado. `MemberRequest` usa `userId`; valida cuenta existente/activa, impide cambiar identidad en PATCH y mantiene datos del expediente.
- **Staff:** POST usa userId preexistente/activo, no name/email. `GET /staff` agrupa roles por persona. PATCH no altera silenciosamente roles múltiples; se bloquea la operación ambigua hasta definir esa política. Se protege owner ante borrado indirecto por otro owner.
- **Repositorio identidad:** eliminado `findOrCreateInvitedUser`. Conservadas `assignStaff`, rol por gimnasio y `setDefaultGymIfMissing` para enlaces reales.
- **Rutinas:** miembros ven únicamente publicadas, filtradas en consulta DB antes de paginar; 404 en lectura por ID no publicada.
- **Ventas:** nuevo `SaleView` y `SaleItemView`, con `items[]`; consulta agrupada con NamedParameterJdbcTemplate evitando N+1; GET y POST responden con líneas del servidor. Checkout mantiene locks/stock/transaction.
- **Calendario:** categoría canónica `ClassCategories` incluye functional; migración aditiva Flyway `V2__allow_functional_calendar_category.sql` amplía los dos CHECK, sin editar V1. Reservas functional cuentan provisionalmente como regulares para limitar repetición diaria.
- **No cambiado:** pagos solo administrativos manuales sin pasarela ni anulación automática; no se implementa recuperación, registro global ni activación de usuarios. Matriz final de permisos y orden de integración Vue pendientes.
- **Pruebas:** creados tests de sesión, enlace de cuentas, roles múltiples, privacidad de rutinas, ítems de ventas y validación de categorías; sin Maven/DB disponibles para ejecutarlos aquí.
- Actualizados `README_MASTER_BACKEND.md`, README, contratos, seguridad, decisiones pendientes y verificación. Incluida guía `PROVISIONAMIENTO_USUARIOS_SQL.md` sin contraseñas reales.

## 2026-11-03 — Bootstrap backend v0.1.0 (entrega previa)

- Construida estructura exclusivamente backend Java según patrón de `app.zip` (interfaces, repositories, services, BaseCRUD, BaseService, AuditService, factoría, routes).
- SQL del frontend copiado a Flyway V1 con `${schema_db}` configurable.
- JWT, roles por gimnasio, CRUD modular inicial, pagos administrativos, venta transaccional, reservas y check-in manual.
- Reservados jobs, notificaciones, archivos cloud y auditoría transversal.
- Pruebas unitarias iniciales y documentación. Validación con Maven/PostgreSQL **pendiente**.


## 03/11/2026 — Refactor JPA de persistencia

Se eliminaron consultas JDBC en repositorios, se incorporaron modelos JPA de tablas existentes, se conservó una única operación nativa Postgres de auditoría, se añadieron pruebas de regresión y un POM reconstruido. Se mantuvieron todos los Controllers y migraciones V1–V4 sin modificaciones. Ver `REFACTORIZACION_JPA_BUENAS_PRACTICAS.md`. Verificación: 188 archivos Java parseados sin errores sintácticos; Maven/DB todavía pendientes.
