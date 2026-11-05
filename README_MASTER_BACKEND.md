> **ESTADO ACTUAL (refactor JPA 03/11/2026):** Se reemplazó el acceso `JdbcTemplate` de los repositorios por JPA/JPQL, conservando BaseCRUD/BaseService y la migración V4. Solo subsiste `set_config` encapsulado en `shared/persistence/repositories/AuditActorRepository` para los triggers PostgreSQL. Ver `docs/REFACTORIZACION_JPA_BUENAS_PRACTICAS.md`. Este ZIP contiene un `pom.xml` reconstruido porque el original nunca se adjuntó; falta verificar `mvn clean test` + PostgreSQL antes de desplegar. Las secciones inferiores que describan JDBC o ausencia de POM son históricas.

> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

# GYM BACK — DOCUMENTO MAESTRO DEL BACKEND

**Archivo maestro para trabajar con cualquier IA:** `README_MASTER_BACKEND.md` (este archivo).  
**Main ejecutable:** `src/main/java/com/gymback/GymBackApplication.java`.  
**Actualizado:** 2026-11-03. **Alcance:** únicamente backend Java; frontend Vue no modificado.  
**Estado de verificación:** cambios implementados en archivos, sin compilación Maven ni pruebas con PostgreSQL real en este entorno. NO declarar integración o producción completadas.

## 1. Prompt de continuidad para cualquier IA

> Trabaja exclusivamente sobre el backend Gym Back, Java 21 + Spring Boot + PostgreSQL. Lee primero este documento, luego `docs/DECISIONES_PENDIENTES.md`, `docs/ARQUITECTURA_Y_PATRONES.md`, `docs/CONTRATOS_FRONTEND.md`, código real del módulo y migraciones Flyway. Mantén `Controller -> Service -> I*Repository -> Repository -> PostgreSQL`, con `BaseCRUD`, `BaseService`, `AuditService` y `ServiceFactory`. Las reglas de negocio viven exclusivamente en Service; no accedas a JdbcTemplate/JPA desde Service o Controller. No crees nuevos endpoints, tablas ni flujos sin requerimiento; no asumas permisos aún no definidos. Las transacciones van en los casos de uso. Verifica `gymId` y rol sobre servidor, filtra `gym_id` en toda consulta, crea pruebas de regresión, conserva la compatibilidad de migraciones Flyway y actualiza docs y registro de cambios. La nueva fuente de verdad es el código actualizado y las decisiones confirmadas debajo; nunca sigas ejemplos desactualizados.

## 2. Decisiones confirmadas por el usuario

1. **Cuentas globales se insertan manualmente mediante SQL** en `${schema_db}.users`, con hash BCrypt compatible y `status='active'`. Registro público, invitaciones, activación y recuperación de contraseña **NO** se implementan ahora.
2. `POST /gyms/{gymId}/members` y `POST /gyms/{gymId}/staff` NO crean usuarios globales: **asocian un `userId` existente y activo** a un gimnasio. PATCH no puede reasignar identidad.
3. **Selección de gimnasio dentro del flujo de login.** Por privacidad, el servidor valida primero email/contraseña y devuelve `gyms[]` con `roles[]` efectivos de cada uno; Vue muestra el selector después de autenticar, pero antes de ingresar a la aplicación. No se implementa un endpoint público de enumeración de gimnasios por email. `gymId` elegido viaja en la URL de APIs tenant y el servidor revalida permisos. `/auth/me` devuelve el mismo modelo `user + gyms` que `/auth/login` (menos el token), para restaurar selector y sesión.
4. `admin` es un rol global y puede consultar gimnasios activos; `owner/trainer/member` son roles por gimnasio, y el mismo usuario puede tener múltiples roles en un mismo gym. **La matriz definitiva de permisos queda pendiente**; conservar permisos preexistentes como política provisional, no asegurar que sean definitivos.
5. Los **pagos son exclusivamente administrativos y manuales**; NO hay pasarela, conciliación bancaria, reembolso ni flujo de corrección automática de capturas equivocadas. La responsabilidad de captura es del operador. `PaymentService` calcula monto y vencimiento desde el plan.
6. **No integrar ni modificar Vue aún**. Los contratos de Vue se adaptarán cuando el backend esté probado; no definir por adelantado el orden de integración de módulos.
7. **Calendario:** la categoría `functional` del frontend queda permitida en SQL y validaciones Java mediante migración V2, sin alterar la V1. Se trata provisionalmente como clase regular a efectos del límite diario de reservas; confirmar esa regla cuando se defina política de calendario.
8. **Rutinas:** un `member` solo consulta `published=true`; owner/trainer/admin pueden consultar borradores, conservando reglas de autorización preexistentes.
9. **Ventas:** `GET /sales` y `POST /sales` devuelven detalle de artículos (ID de producto, nombre al vender, cantidad, precio unitario y total de línea). Nunca se aceptan precios de cliente.
10. `schema_db` (alias `SCHEMA_DB`) decide esquema de PostgreSQL; no hardcodear `test` en Java ni SQL parametrizado por cliente.

## 3. Arquitectura y estructura

- `security/`: JWT, autenticación, modelo `dto/AuthUser`, `dto/GymAccess`, `dto/AuthSession`, `dto/LoginResponse`, autorización y `IIdentityRepository`/`IdentityRepository`.
- `modules/members/`, `modules/staff/`: relación con cuenta global existente y operaciones del gimnasio.
- `modules/payments/`: registro administrativo manual.
- `modules/calendar/`: clases, plantillas, reservas y categoría canónica (`ClassCategories`).
- `modules/routines/`: agregado rutina+días+ejercicios; filtro publicado en repositorio.
- `modules/pos/`: productos, venta transaccional e ítems con lectura agrupada.
- `modules/gyms/`, `plans/`, `checkin/`, `settings/`, `landing/`, `media/`: código existente, no ampliado aquí.
- `modules/notifications/`, `jobs/`, `files/`, `audit/`: capacidades futuras, no simular completitud.
- `shared/`: bases CRUD/service, auditoría acotada a columnas reales, factoría, errores.
- `src/main/resources/db/migration/V1__base_schema.sql`: creación original; NO editar después de aplicar.
- `src/main/resources/db/migration/V2__allow_functional_calendar_category.sql`: extensión de categorías de calendario.

## 4. Contratos esenciales

`POST /api/v1/auth/login` con `{ "email": "...", "password": "..." }`: `{ "token":"...", "tokenType":"Bearer", "user":{ "id", "name", "email", "platformAdmin", "defaultGymId" }, "gyms":[{"id","name","slug","roles":["owner", "trainer"]}] }`.

`GET /api/v1/auth/me`: `{ "user":{...}, "gyms":[...] }`; no devuelve token ni un rol global `user.role`. El selector debe permitir elegir entre los IDs autorizados; solo admin puede ver todos los gimnasios activos.

`POST /api/v1/gyms/{gymId}/members`: `{ "userId":"UUID_EXISTENTE", "code":"M-001", "active":true }`. La respuesta expone perfil de miembro, no contraseña.

`POST /api/v1/gyms/{gymId}/staff`: `{ "userId":"UUID_EXISTENTE", "role":"trainer" }`. `PATCH /staff/{userId}` `{ "role":"trainer" }`; si ya existen dos roles, no sustituir uno arbitrariamente. `GET /staff` agrega `roles[]` por usuario.

`GET /api/v1/gyms/{gymId}/routines`: para member solo rutinas publicadas; `GET /{id}` de borrador para member devuelve 404.

`GET /api/v1/gyms/{gymId}/sales`: cada venta incluye `items[]` con `productId,productName,quantity,unitPrice,lineTotal`.

`GET /api/v1/gyms/{gymId}/payments`, `POST /api/v1/gyms/{gymId}/payments`: registro manual, sin APIs bancarias ni anulación automática.

Documentación completa en `docs/CONTRATOS_FRONTEND.md`. **No agregar endpoints adicionales por intuición.**

## 5. Ejecutar y verificar

1. JDK 21, Maven 3.9+, PostgreSQL 15+. Asegurar que el rol de DB pueda crear esquema/extensiones o aprovisionarlas previamente.
2. Configurar `.env` (`DB_*`, `schema_db`, `JWT_SECRET` aleatorio >=32 bytes, `FRONTEND_ORIGIN`). Cuentas existentes se crean por SQL; `BOOTSTRAP_ADMIN_*` es una alternativa opcional para el primer administrador, debe quedar vacío si se gestiona todo por SQL.
3. Base limpia: Flyway aplica V1 y V2. Base existente: verificar **historial Flyway** antes de ejecutar y **no repetir V1** sobre tablas existentes.
4. Desde la raíz: `mvn clean test`, `mvn spring-boot:run`. O Docker: `docker compose up --build` tras configurar secretos.
5. `GET /api/v1/health`; login con cuenta real activa y contraseña BCrypt, luego `/auth/me` con bearer.
6. Probar aislamiento entre gimnasios, roles múltiples, links a `userId` existente, visibilidad de rutinas, detalles de ventas y reserva `functional`.
7. Revisar `docs/VERIFICACION.md` para casos obligatorios aún no ejecutados.

## 6. Índice

- `docs/ARQUITECTURA_Y_PATRONES.md` — dependencia de capas y reglas antiparches.
- `docs/CONTRATOS_FRONTEND.md` — contratos backend existentes para la integración posterior.
- `docs/DECISIONES_PENDIENTES.md` — qué NO está definido / qué queda fuera de alcance.
- `docs/SEGURIDAD_Y_BD.md` — aislamiento, roles, login y esquema.
- `docs/PROVISIONAMIENTO_USUARIOS_SQL.md` — alta manual de usuarios y vínculos con gimnasios (guía sin credenciales reales).
- `docs/VERIFICACION.md` — tests ejecutables y pruebas E2E necesarias.
- `docs/REGISTRO_CAMBIOS.md` — evidencia de cambios y límites de validación.

## 7. Regla diaria

Cambiar **código + pruebas + contrato + documentación + migración aditiva** juntos cuando corresponda. Antes de declarar una función «terminada», compilar, probar la migración contra PostgreSQL y comprobar la petición HTTP real. Distinguir hechos verificados de supuestos. No solucionar un problema de service metiendo lógica en controllers.


### Corrección de compilación (2026-11-03)
Se resolvieron dos ambigüedades de consulta de `StaffRepository` y se completó el contrato `insertPlatformAdmin` requerido por `AdminBootstrap`. Ver `docs/REGISTRO_CAMBIOS.md` y correr `mvn clean test` localmente.

## 8. Ampliación propuesta e implementación parcial del 03/11/2026

Se añadieron las rutas de publicación pública y otras consultas/altas guiadas solicitadas. **Leer `docs/INTEGRACION_BACKEND_ESTADO.md` antes de conectar Vue**: describe qué endpoints están implementados, cuáles requieren habilitar `USER_API_ENABLED` (apagada por defecto) y qué queda pendiente (QR, upload, invitados). La decisión original de aprovisionamiento SQL sigue vigente por defecto; se requiere revisión antes de activar altas por API. La migración nueva es `V3__landing_publications.sql`, sin editar V1/V2. La verificación de sintaxis no equivale a `mvn test` ni a pruebas PostgreSQL. El ZIP de entrada contenía código y docs, pero **no incluía pom.xml**.


## Actualización auditoría / refresh / archivos (03/11/2026)

La implementación nueva se describe en `docs/CAMBIOS_IMPLEMENTADOS_AUDITORIA.md` y los contratos para Vue/Pinia en `docs/GUIA_FRONTEND_SESION_IMAGENES.md`. La migración **V4** agrega campos de auditoría y soft-delete al esquema, introduce refresh token rotativo y almacenamiento local de imágenes. Las notas antiguas que describían estos módulos como reservados o los archivos como no implementados son históricas, no representan esta entrega. No se reintroduce publicar/despublicar landing: sigue activa por slug. El ZIP no incluye el `pom.xml` original y la integración real debe verificarse con Maven/PostgreSQL antes de desplegar.
