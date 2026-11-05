> **Documento histórico de la primera implementación V4.** La refactorización posterior eliminó `JdbcTemplate`. Para la implementación actual, consultar `docs/REFACTORIZACION_JPA_BUENAS_PRACTICAS.md`.

# Implementación backend — auditoría universal, soft-delete, refresh token e imágenes

**Fecha:** 03/11/2026 | **Archivo de origen:** `src.zip` del usuario | **Aplicación:** Java 21, Spring Boot, JPA/Hibernate, `JdbcTemplate`, Flyway, PostgreSQL 15+.

## 1. Resumen ejecutivo

Se modificaron los componentes existentes siguiendo `Route -> Service -> Interface -> Repository -> BD`, reutilizando `BaseService`, `BaseCRUD`, `AuditService`, `ServiceFactory`, `AuthorizationService`, las entidades, la tabla `user_sessions` y los contratos de seguridad actuales.

- `V4__universal_audit_soft_delete.sql` añade **seis columnas** (`created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`) a **todas las tablas existentes del esquema PostgreSQL** (29 tablas de V1+V3 en la instalación base). Mantiene V1–V3 inalteradas, rellena timestamps faltantes y crea triggers de auditoría + protección contra `DELETE` físico para tablas de negocio. La tabla Flyway interna queda excluida intencionalmente.
- `AuditContext` transmite `actor.id` mediante `set_config('app.actor_id', ..., true)` en la **misma transacción**; los triggers completan los campos y añaden un evento por `INSERT`/`UPDATE`/soft-delete, también para repositorios SQL directos. No se inventa `created_by` de filas anteriores.
- Modelos JPA actuales contienen los campos nuevos y `@SQLRestriction("deleted_at IS NULL")`; `BaseCRUD` filtra `get`, `list`, `getWhere` y ya no ejecuta `EntityManager.remove`. Las consultas `JdbcTemplate` y las vistas efectivas fueron ajustadas para no devolver borrados.
- Los `DELETE` existentes archivan/inactivan/marcan borrado lógico según reglas previas. El reemplazo de días y ejercicios de rutinas ahora deja historial, y se adecuan **índices únicos parciales** que antes hacían inviable volver a guardar órdenes después de archivar estructura.
- Nuevo login con refresh de 30 días, secreto aleatorio opaco de 48 bytes (base64url), SHA-256 persistido, rotación de un solo uso en transacción con `FOR UPDATE`; logout revoca la sesión. Access JWT conserva su duración de 60 minutos.
- Upload local de imágenes con subcarpeta por `gymId` bajo `${GMS_FILES_ROOT:/var/lib/gms/files}`, claves aleatorias y validación de tamaño/firma PNG/JPG/WebP. Contenido privado protegido por bearer y pública **solo** para activos explícitamente referenciados en bloques visibles o logo del gym activo.
- Lectura de `audit_events` limitada a owner del gimnasio / admin global. No se añaden modificaciones ni borrados del log.

## 2. Archivos modificados o creados y razón

| Área | Archivos/ruta | Razón |
|---|---|---|
| Flyway | `src/main/resources/db/migration/V4__universal_audit_soft_delete.sql` **nuevo** | Datos históricos, columnas, triggers, `v_user_roles` y `v_member_access`, índices parciales, impedir borrado físico. |
| Genéricos | `shared/repositories/BaseCRUD.java`; `shared/services/BaseService.java`, `AuditService.java` **reutilizado**, `AuditContext.java` **nuevo** | Reutilizar filtro y auditoría en CRUD y evitar `em.remove`. |
| Modelos | `modules/{gyms,members,plans,calendar,landing,media,pos,routines}/model/*` (nueve entidades) | Mapear seis campos en JPA y excluir borrados por defecto. |
| Servicios | `modules/{gyms,members,plans,calendar,landing,media,pos,routines}/services/*` | Mantener reglas de negocio existentes y sustituir eliminación por soft-delete. |
| Repositorios SQL | `security/IdentityRepository.java`, `modules/{users,staff,members,calendar,payments,checkin,pos,settings,dashboard,landing,gyms,routines}/repositories/*` | Filtrar `deleted_at` en lecturas/ediciones, relaciones y conteos. |
| Seguridad | `security/{AuthController,AuthService,AuthorizationService,SecurityConfig,IRefreshTokenRepository,RefreshTokenRepository}.java`; `security/dto/LoginResponse.java` | Refresh opaco rotativo, rutas permitidas y propagación de actor. |
| Multimedia | `modules/media/{interfaces,model,repositories,routes,services}`, `modules/landing/{interfaces,repositories,services,routes}` y nuevo `modules/files/{interfaces,repositories,services,routes}` | Upload local y entrega autorizada sin SQL en controller/service. |
| Auditoría lectura | nuevo `modules/audit/{interfaces,repositories,services,routes}` | Historial acotado por rol y gimnasio. |
| Inyección/infra | `shared/factories/ServiceFactory.java`; `src/main/resources/application.yml` | Inyectar servicios y configurar multipart/directorio. |
| Pruebas | `src/test/java/com/gymback/security/{AuthServiceTest,RefreshTokenTest}.java` y `src/test/java/com/gymback/files/LocalImageStorageTest.java` | Validaciones unitarias nuevas y compatibilidad del test anterior. |
| Integración frontend | `docs/GUIA_FRONTEND_SESION_IMAGENES.md` | Contratos, requests, respuestas, errores, consideraciones Vue/Pinia. |

**Arquitectura conservada:** controller convierte HTTP; service coordina permisos/transacción; interfaces exponen capacidades; repositorios realizan SQL/JPA; Flyway maneja esquema. `AuditContext` y triggers complementan a `AuditService` para cubrir las escrituras que pasan por JDBC.

## 3. Base de datos: alcance y decisiones

- La migración agrega las columnas por iteración sobre `pg_catalog.pg_tables` de `${schema_db}` (no hardcodear `test`). Todas las tablas *ya existentes cuando corre V4* reciben los campos. **Tablas creadas después de V4 necesitan registrar su misma política en otra migración.**
- `created_at`, `updated_at`: `timestamptz NOT NULL DEFAULT now()`; se rellenan `NULL` históricos con la hora de migración. Los timestamps preexistentes se conservan.
- `created_by`, `updated_by`, `deleted_by`: UUID nulos permitidos, referencia a `users(id) ON DELETE SET NULL`, acordes con procesos de sistema. `deleted_at`: `timestamptz NULL` (NULL = vivo).
- Trigger `row_audit_metadata`: creación pone timestamps y actor, actualización conserva creación y actualiza `updated_*`, borrado lógico pone `deleted_by`. Se contempla recuperación explícita de `gym_staff_roles` al reasignar.
- Trigger `row_audit_event`: una fila por creación/actualización/borrado/restauración, con clave simple o compuesta en `entity_key` JSONB. **No guarda `old_values`/`new_values`**: evita replicar contraseñas, tokens y datos sensibles en el log. No audita su propia inserción para evitar recursión.
- Trigger `block_physical_delete`: rechaza `DELETE` físico de filas de tablas del esquema, incluidos logs y relaciones con cascada. Una eliminación solicitada por API es siempre soft-delete o una transición de estado semántica como cancelación de reserva.
- `v_user_roles`, `v_member_access`: excluyen gimnasio, usuario, relación, membresía y suscripción borrados para evitar acceso residual.
- Índices parciales para nombres de planes, miembros, SKU, código, órdenes rutina, reservaciones activas, ingresos diarios y otros identificadores de negocio; los **PK y UNIQUE utilizados por FKs permanecen**. Slug y email se mantienen reservados incluso si hay borrado lógico.

**No se modificaron V1, V2 ni V3.** En una base instalada, Flyway aplicará solo V4. Asegurarse de que no hubo alteraciones manuales a `flyway_schema_history`.

### Operación y precauciones

1. **Backup** consistente de BD, preferentemente entorno de staging restaurado.
2. Revisar el espacio y bloqueos: V4 hace `ALTER TABLE`, crea múltiples índices, y al habilitar triggers aumenta escrituras en `audit_events`. Programar una ventana de mantenimiento si hay datos grandes.
3. Ejecutar con la identidad de BD que ya aplica Flyway y permisos sobre el esquema definido. No ejecutar V4 manualmente y luego esperar que Flyway la detecte.
4. Probar registros existentes con historial `created_by = NULL`; **no atribuir** registros previos al usuario que ejecutó la migración.
5. Supervisar crecimiento de `audit_events`; definir retención legal y operativa. Los archivos eliminados lógicamente **no se borran físicamente** y necesitan una política de retención revisada antes de producción.

## 4. Renovación de sesión

- `POST /api/v1/auth/login` -> conserva `token`, `tokenType`, `user`, `gyms`; añade `refreshToken`, `expiresIn` (3600).
- `POST /api/v1/auth/refresh` body `{"refreshToken":"..."}` -> 200 respuesta de login con ambos tokens nuevos. Usa bloqueo `SELECT FOR UPDATE` del hash anterior, lo revoca, emite otro: dos refresh simultáneos con el mismo secreto no deben prosperar.
- `POST /api/v1/auth/logout` body anterior -> 204, revoca el refresh. Sin blacklist de access JWT actual.
- Se reutiliza `user_sessions` de V1; no se guarda refresh en claro y no se añaden credenciales al log de auditoría. 401 para token inválido/expirado/revocado o usuario deshabilitado.
- Debe aplicarse HTTPS fuera de local, protección contra XSS, secretos JWT fuertes, rate limiting para login/refresh a nivel de gateway (no implementado aquí). La implementación actual **no** envía cookies HttpOnly.

## 5. Almacenamiento local y rutas

Variable `GMS_FILES_ROOT=/var/lib/gms/files` (valor por defecto). Se almacenan archivos como `/var/lib/gms/files/<gym-uuid>/<uuid-archivo>.jpg|png|webp`; solo la **clave relativa** queda en `media_assets.storage_key`, mientras `external_url=NULL`. El directorio no se expone directamente con Nginx/static Spring ni se permite path suministrado por el cliente. `MediaAsset` ofrece `contentUrl` en su JSON; para redes públicas usar `/api/v1/public/media/{id}` y autorización de bloque/logo.

| Endpoint | Seguridad | Acción |
|---|---|---|
| `POST /api/v1/gyms/{gymId}/media-assets/upload` | owner/admin, multipart `file`+`name` | Crea asset y escribe el archivo. |
| `GET /api/v1/gyms/{gymId}/media-assets/{id}/content` | owner/trainer o miembro dueño de su foto | Devuelve bytes (requiere Bearer). |
| `GET /api/v1/public/media/{id}` | anónimo, solo referencia pública válida | Devuelve bytes de imagen autorizada en landing/logo. |
| `GET /api/v1/gyms/{gymId}/media-assets` | contrato anterior | Lista assets vigentes, con `contentUrl`. |
| `PATCH /api/v1/gyms/{gymId}/landing/logo` | owner/admin, JSON `imageAssetId` | Configura como logo una imagen local existente del mismo gym. |
| `DELETE /api/v1/gyms/{gymId}/media-assets/{id}` | owner/admin | Marca borrado en BD; conserva archivo físico para retención. |

- Verifica firma del archivo (PNG/JPEG/WebP), ignora nombre de archivo de cliente para decidir ruta, limita streaming a 8 MiB y descarta archivo en rollback de transacción. `max-file-size` 8MB y `max-request-size` 9MB en `application.yml`.
- Queda pendiente proceso externo de limpieza de archivos huérfanos después de caídas abruptas/errores de commit y política de retención; no se habilitó tarea automática para no perder históricos.
- De producción: `sudo install -d -o <usuario-app> -g <grupo-app> -m 0750 /var/lib/gms/files`, montar volumen persistente en contenedores, provisionar backup y verificar permisos de lectura/escritura del proceso Java. **No** servir `/var/lib/gms/files/` directamente vía Nginx.
- Si frontend y API usan hosts distintos, anteponer el origen de la API a URLs relativas de `contentUrl`/`mediaAssets[].url`.

## 6. Endpoints nuevos de auditoría

- `GET /api/v1/gyms/{gymId}/audit-events?offset=0&limit=50`: owner (admin global permitido).
- `GET /api/v1/audit-events?gymId={uuid}&offset=0&limit=50`: solo admin global, filtro `gymId` opcional.
- `limit` entre 1 y 100. Orden cronológico descendente. No se expone endpoint para borrar el log.

## 7. QA requerida (no afirmar producción aún)

**Ejecución disponible en este entorno:** parsing de sintaxis Java 21 sobre todos los `.java` y comprobación estructural del ZIP, **sin errores de sintaxis**. No se ejecutó compilación Maven, JUnit, ni integración PostgreSQL, porque el ZIP recibido contiene `src/` + `docs/` pero **no trae `pom.xml`** y en este entorno no están disponibles Maven/PostgreSQL. Por tanto los cambios son **código preparado, no integración validada en ejecución**.

Con el `pom.xml` original y PostgreSQL instalado:

```bash
mvn clean test
mvn spring-boot:run
# en staging, comprobar flyway_schema_history y columnas para las 29 tablas.
```

Pruebas críticas:

1. `V4` aplicada con éxito en BD vacía y en una restaurada de producción; confirmar triggers e índices parciales.
2. Un owner de gimnasio A inserta/actualiza/elimina miembro/producto/landing. DB conserva row con actor y timestamps; GET lista, detalle, dashboard, public landing y vistas no exponen borrados; gym B no accede.
3. Replicar inserciones JDBC (pago, venta, check-in, settings, roles) y comprobar `created_by` y filas de `audit_events`, con actor real.
4. Editar una rutina con días anteriores y mismo `day_order`: no violar UNIQUE, solo se leen nuevos días/ejercicios, antiguos `deleted_at` quedan marcados.
5. `DELETE FROM test.products ...` desde SQL debe fallar con error físico intencional. Uso permitido: `UPDATE ... SET deleted_at=now()` por personal autorizado.
6. Login/refresh/logout válido; intentar usar refresh antiguo, doble refresh concurrente, refresh vencido y usuario deshabilitado.
7. Subir PNG/JPG/WebP, inválido, ruta `../`, >8 MB, rol no permitido; verificar persistencia y que el navegador no pueda leer fotos privadas sin autorización.
8. Insertar referencia a imagen local en bloque landing habilitado: contenido público 200; bloque deshabilitado o borrado, gimnasio suspendido, recurso eliminado/no referenciado -> 404.
9. Migración de entorno con schema no `test` (por ejemplo `qa`), registro de auditoría y tabla `audit_events` bajo schema correcto.
10. Verificar monitorización de carga de escritura: cada actualización produce un evento adicional; medir en staging antes de producción.

**Decisiones no abordadas:** restore genérico (no API), eliminación física regulatoria después de vencimiento de retención, rate-limiting y almacenamiento remoto/S3, revocación inmediata de access JWT y cookies HttpOnly. No describir estas funciones como implementadas.

## 8. Frontend

Todo el contrato y los cambios de Axios/Pinia, componentes multimedia, landing y manejo de borrado están en [`docs/GUIA_FRONTEND_SESION_IMAGENES.md`](GUIA_FRONTEND_SESION_IMAGENES.md). **No quitar otras funciones ni reescribir textos originales de las pantallas.**
