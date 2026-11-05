# Refactor arquitectónico JPA — Gym Back

**Base exacta:** `gym_back_auditoria_refresh_imagenes.zip` (entrega anterior), no el ZIP previo de `src.zip`. **Fecha:** 2026-11-03. **Ámbito:** backend completo. **Frontend:** sin modificar. **Versión de base de datos:** V1–V4 **idénticas** (no se necesita V5 porque no cambió el esquema).

## 1. Resultado y evidencia real

La capa de persistencia dejó de utilizar `JdbcTemplate` y `NamedParameterJdbcTemplate`. Las 20 clases que dependían de SQL directo o SQL nativo fueron revisadas y sus operaciones de negocio se pasaron a entidades JPA, JPQL/HQL, bloqueos de JPA y transacciones en servicios. La única operación SQL nativa que se mantiene en código Java es **`select set_config('app.actor_id', :actor, true)`** y se encuentra exclusivamente en `shared/persistence/repositories/AuditActorRepository.java`. Esta llamada es el mecanismo PostgreSQL que utiliza V4 para atribuir el actor real a los triggers de auditoría; sustituirla por un `UPDATE` común modificaría el contrato técnico de auditoría. No se utiliza para CRUD ni consultas de negocio.

**Verificaciones efectuadas en este entorno:**
- Comparación archivo por archivo: **0 archivos faltantes** respecto del último ZIP; las 25 clases Controller conservan **exactamente los mismos bytes**; `application.yml` y V1, V2, V3 y V4 conservan sus bytes.
- La búsqueda de `JdbcTemplate` y `NamedParameterJdbcTemplate` en Java no encontró usos.
- Java 21 (API del compilador `JavacTask.parse`): **188 archivos Java sintácticamente válidos**, incluidos los tests existentes y los nuevos.
- Añadidas cinco pruebas unitarias de regresión en `src/test/java/com/gymback/persistence/JpaRepositoryRegressionTest.java`.

**Verificaciones no efectuadas**: compilación Maven con dependencias, tests JUnit/Mockito, `ddl-auto=validate` en PostgreSQL, migraciones Flyway en un servidor real y pruebas HTTP multiusuario. Por tanto **no se afirma compatibilidad integral probada ni preparación para producción**. La fuente entregada permite correrlas en el entorno del proyecto.

## 2. Patrón de arquitectura que permanece

```text
HTTP / Controller (contratos preexistentes, sin cambios)
              ↓
Service (reglas de negocio, validación y @Transactional)
              ↓
I*Repository (interfaces/contratos sin SQL)
              ↓
Repository (BaseCRUD para CRUD estándar; JPA para consultas/agregados)
              ↓
EntityManager / Hibernate 6 / PostgreSQL
```

- `BaseCRUD<T>` y `BaseService<T>` siguen siendo las abstracciones para CRUD genérico, ámbito `gymId`, filtros de `deletedAt`, paginación, creación, actualización y borrado lógico.
- Los repositorios con **casos especializados** (venta compuesta, check-in, auditoría, reservas, identidad o métricas) implementan su interfaz, pero no se fuerzan artificialmente a heredar de `BaseCRUD` si no representan un CRUD de una sola entidad.
- Se añadieron modelos persistentes en `shared/persistence/model/` para tablas existentes antes no mapeadas, y `AuditedRecord` como `@MappedSuperclass` para reutilizar seis campos V4. **No hay tablas nuevas**.
- `@SQLRestriction("deleted_at IS NULL")` oculta registros archivados a las consultas JPA; `GymStaffRole` **no lleva** esa anotación por una razón precisa: necesita recuperar la misma clave primaria en un flujo de reasignación y filtra manualmente los registros vivos en consultas normales.
- `GymSlugRegistry` es lectura sin filtro, para respetar la restricción `UNIQUE` física de `citext` incluso si el gimnasio está archivado. No promete reutilizar el slug de un registro archivado.
- `MemberAccessView` se mapea como entidad `@Immutable` sobre la **vista PostgreSQL existente** `v_member_access`. No se reimplementa de modo incompleto su regla de acceso.
- `AuditActorView` consulta los nombres históricos de usuarios eliminados lógicamente en `audit_events`, sin permitirles iniciar sesión.
- Las operaciones de auditoría de cambios siguen pasando por `AuditContext` y el trigger de V4: `AuditContext` solo coordina, `AuditActorRepository` ejecuta la operación PostgreSQL dentro de la misma transacción JPA.

## 3. Repositorios revisados y justificación

| Módulo / archivo | Antes | Ahora |
|---|---|---|
| `security/IdentityRepository` | SQL de usuarios, roles y UPSERT de staff | JPA de usuario/roles, consulta por gimnasio, restauración del rol con PK compuesta |
| `security/RefreshTokenRepository` | SELECT/INSERT/UPDATE SQL | Entidad `UserSession`, bloqueo `PESSIMISTIC_WRITE`, revocación y rotación |
| `security/AdminBootstrap` | INSERT de usuario con JDBC | Persistencia JPA de usuario y vinculación al rol administrador |
| `users/UserRepository` | SQL dinámico y `pg_advisory_xact_lock` | JPQL con parámetros y whitelist; locks JPA del conjunto de admins |
| `staff/StaffRepository` | JOIN SQL y agrupamiento manual | JPQL con JOIN + agrupación de roles con contrato JSON igual |
| `members/MemberRepository` | SQL de cinco tablas | JPQL con JOINs, proyección a mapas de salida conservando claves |
| `payments/PaymentRepository` | SQL de planes, suscripciones y pagos | Modelos JPA, bloqueos de suscripción y miembro, listado tipado |
| `calendar/GymClassRepository` | SQL con timezone y recuperación por ID | JPQL sobre `GymClass`, conversión de rangos de día usando `ZoneId` del gimnasio |
| `calendar/ReservationRepository` | SQL de cupos, reservas y cancelación | JPA y bloqueo pesimista de clase, fecha local y vista de acceso |
| `checkin/CheckinRepository` | SQL para registro/lectura | Entidad `CheckinEntry`, consultas JPQL y `MemberAccessView` |
| `pos/SaleRepository` | SQL + NamedParameterJdbcTemplate de ítems | Entidades de venta, ítems e inventario; consultas por lotes |
| `pos/ProductRepository` | INSERT nativo de inventario | `em.persist(InventoryMovement)` sin SQL textual |
| `routines/RoutineStructureRepository` | SQL de borrado/alta/listado de días | JPA: valida primero, archiva días/ejercicios, hace `flush`, crea nuevos |
| `settings/SettingsRepository` | UPDATE con concatenación dinámica de nombres de columna | Mapeo explícito de campos permitidos en `GymConfiguration` |
| `landing/LandingPublicationRepository` | SQL de gimnasio, opciones y bloques | Consultas y actualización de entidades; página viva por slug |
| `files/MediaAccessRepository` | SQL JSONB y control de fotos | JPA de bloques activos, gimnasio y miembros; verificación de referencias |
| `dashboard/DashboardRepository` | SQL con funciones agregadas y zonas | JPQL de conteos/sumas con rangos calculados en hora local |
| `audit/AuditEventRepository` | SQL y alias de campos | JPQL de eventos, actor y proyección existente |
| `gyms/GymRepository` | SQL para duplicados de slugs | JPQL sobre `GymSlugRegistry` (considera archivados) |
| `shared/services/AuditContext` | Acceso JDBC desde la capa de servicios | Delega al repositorio especializado de contexto de auditoría |

## 4. Garantías preservadas y mejoras

**Auditoría y borrado lógico:** Se conservaron migración V4, triggers, eventos, columnas `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`, endpoints e interfaces. No hay sentencias `DELETE FROM` en Java. El borrado de rutinas archiva las subfilas antes de insertar reemplazos; la eliminación de staff marca inactivo y archivado, mientras reasignarlo restaura su fila. Las consultas JPA aplican filtros de registros activos.

**Concurrencia:**
- Reserva de clase: lock pesimista antes de contar cupos y crear reserva.
- Ventas: lock pesimista de cada producto antes de verificar stock; una consulta para todos los ítems.
- Pago: lock del miembro y de la suscripción activa, evitando doble alta concurrente cuando aún no tiene plan.
- Refresh token: lock de sesión para rotación de un solo uso.
- Desactivación del último admin: lock JPA de asignaciones de administrador en orden estable antes de contar.
- Cambio de slug: lock del gimnasio al modificarlo, y comprobación de unicidad; PostgreSQL conserva su constraint `UNIQUE`.

**Multi-gimnasio y privacidad:** Los `gymId` se comprueban en Services usando `AuthorizationService`, y los repositorios filtran los datos por gimnasio. Las fotos privadas solo se entregan al actor autorizado; las públicas exigen referencia explícita en la landing habilitada o en el logo de un gimnasio activo. Los roles son globales o por gimnasio como antes y no se restablece publicar/despublicar la landing.

**Contratos frontend:** No se cambió ninguna ruta `Controller`, request DTO ni interfaz pública de repositorio. Se mantienen `POST /api/v1/auth/refresh`, revocación de sesión, subida/lectura de imágenes, URL por slug y consultas existentes. La guía de frontend **vigente** sigue siendo `docs/GUIA_FRONTEND_SESION_IMAGENES.md`. Las claves snake_case que retornaban las consultas de listados SQL se reconstituyeron en los mapas para no forzar un cambio de frontend.

## 5. POM y empaquetado

El ZIP anterior **no traía `pom.xml`**. Para entregar una raíz Maven utilizable, este paquete contiene un `pom.xml` **reconstruido** con Java 21, Spring Boot 3.4.11, JPA, security, JWT resource server, Flyway PostgreSQL, PostgreSQL driver y JUnit/Mockito. **No es una copia del `pom.xml` local del usuario.** Antes de sustituir el build real, compara ambas versiones, plugins, perfiles, propiedades y dependencias, y conserva las particularidades del proyecto local.

Se añadió `.env.example` **sin credenciales reales** y `.gitignore`. Los scripts auxiliares usados durante la refactorización no forman parte del paquete final.

## 6. Verificación obligatoria en máquina con Maven y PostgreSQL

1. Respaldar la base y probar primero contra una copia. Usar JDK 21 y Maven 3.9+.
2. Fusionar/verificar `pom.xml` con el POM real si ya existía. Configurar `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `schema_db`, `JWT_SECRET`, `PUBLIC_SITE_BASE_URL`, `FRONTEND_ORIGIN` y `GMS_FILES_ROOT`.
3. Ejecutar `mvn clean test` y resolver cualquier incompatibilidad de versión de dependencias que el POM local manifieste. **Este comando no se ejecutó aquí.**
4. Ejecutar `mvn spring-boot:run` y verificar que Hibernate complete `ddl-auto=validate`. No cambiar el esquema manualmente para hacer coincidir los modelos; corregir el mapeo si algo difiere.
5. Confirmar Flyway en `flyway_schema_history`: V1–V4 con checksums idénticos; no editar ni reejecutar migraciones antiguas. Un proyecto con V4 aplicada debe arrancar sin nueva migración.
6. Probar login, selector de gimnasios, `/auth/me`, refresh dos veces con el mismo token (la segunda debe fallar), logout, roles múltiples de staff y reactivación de rol archivado.
7. Probar CRUD y soft-delete de gimnasio, miembro, plan, rutina, clase, producto y medios: el registro archivado debe dejar de aparecer en GET sin desaparecer físicamente de PostgreSQL; verificar `created_by`, `updated_by`, `deleted_by`.
8. Probar venta de dos productos, transacciones paralelas con stock justo, total y líneas; pago y doble pago concurrente; reserva y dos reservas simultáneas al último cupo; check-in duplicado y fechas de calendario en zona del gimnasio.
9. Probar landing `/g/{slug}` con gimnasio activo/inactivo, bloques habilitados, logo público, imagen privada e imagen propia de miembro. Verificar que nadie pueda leer archivos ajenos por UUID.
10. Comparar respuesta JSON por endpoint con el frontend existente, especialmente listados de pagos, check-ins, membresías, staff y métricas. Ejecutar tests HTTP/integ y revisar SQL que genere Hibernate.

## 7. Limitaciones y criterios para dar por terminada la migración

La validación sintáctica **no sustituye** a la compilación ni a la conexión de Hibernate con PostgreSQL. Se necesita verificar JPQL/HQL, mappings JSONB y CITEXT, `@SQLRestriction`, `@IdClass`, `@Immutable`, locks `PESSIMISTIC_WRITE`, índices y latencia con datos reales. No anunciar «no rompe nada» hasta superar el punto 6. Los controles de seguridad por rol son los vigentes; no se añadió una matriz nueva. **No se cambió ninguna migración ni se reintrodujeron endpoints de publicar/despublicar.**
