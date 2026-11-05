> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

# Integración backend — estado de implementación (03/11/2026)

**Entrada:** `docs/ESPECIFICACION_ENDPOINTS_PROPUESTA.md` (28 rutas propuestas), código del ZIP Java y guías `README_MASTER_BACKEND.md`, `docs/ARQUITECTURA_Y_PATRONES.md`, `docs/SEGURIDAD_Y_BD.md`.

**Alcance de esta entrega:** implementación de **18 rutas de las 28 propuestas** en Java, más correcciones puntuales de rutas existentes. **No se cambió Vue.** Los endpoints de cuentas/registro (8) se incluyen **deshabilitados por defecto** para mantener la decisión original de provisionamiento manual por SQL. No afirmar que la aplicación está lista para producción: falta compilación Maven, migración en PostgreSQL y prueba HTTP real. El ZIP recibido **no incluye `pom.xml`**, y el entorno de preparación no dispone de `mvn`.

## 1. Inventario exacto

| ID | Nuevo endpoint (prefijo `/api/v1`) | Estado de código / condición |
|---|---|---|
| L01 | `GET /gyms/{gymId}/landing/publication` | Implementado, owner/admin |
| L02 | `POST /gyms/{gymId}/landing/publish` | Implementado, snapshot, versión y rollback transaccional |
| L03 | `POST /gyms/{gymId}/landing/unpublish` | Implementado, sin borrar borrador |
| L04 | `PATCH /gyms/{gymId}/landing/slug` | Implementado, slug único, reservado y normalizado |
| L05 | `GET /public/gyms/{slug}/landing` | Implementado, público sin JWT, estado activo + publicación + snapshot |
| U01 | `POST /users` | Código implementado, requiere `USER_API_ENABLED=true` |
| U02 | `GET /users` | Código implementado, requiere `USER_API_ENABLED=true` |
| U03 | `GET /users/{id}` | Código implementado, requiere `USER_API_ENABLED=true` |
| U04 | `PATCH /users/{id}` | Código implementado, requiere `USER_API_ENABLED=true` |
| U05 | `PATCH /users/{id}/status` | Código implementado, requiere `USER_API_ENABLED=true` |
| U06 | `GET /users/search?email=...&gymId=...` | Código implementado, requiere `USER_API_ENABLED=true`; owner solo encuentra cuentas vinculadas a ese gym; admin global puede buscar cualquier cuenta por email exacto |
| M01 | `POST /gyms/{gymId}/members/register` | Código implementado, requiere `USER_API_ENABLED=true` |
| M02 | `GET /gyms/{gymId}/members/me` | Implementado; usuario autenticado con rol member activo |
| S01 | `POST /gyms/{gymId}/staff/register` | Código implementado, requiere `USER_API_ENABLED=true`; owner solo registra trainer |
| R01 | `GET /gyms/{gymId}/reservations/me` | Implementado, filtros por fecha/estado y paginación |
| R02 | `GET /gyms/{gymId}/classes/{classId}/availability` | Implementado; las reservas siguen revalidando cupos al confirmar |
| C01 | `GET /gyms/{gymId}/checkins/me` | Implementado, historial propio y filtro de fechas |
| D01 | `GET /gyms/{gymId}/dashboard/summary` | Implementado, agregaciones SQL de todo el periodo, owner/admin |
| F01 | `POST /gyms/{gymId}/media-assets/upload` | **Pendiente:** proveedor de almacenamiento, URLs seguras, segregación foto privada/landing |
| Q01–Q06 | Dispositivos y check-in QR/kiosco | **Pendientes:** mecanismo seguro de pairing/credenciales, revocación y rate limiting; la DB ya tiene estructuras |
| G01–G03 | Guest-passes | **No implementados** intencionalmente: la pantalla actual sigue informativa; decisión futura |

**Total de rutas nuevas creadas:** 18. **Rutas pendientes:** 10. Las rutas existentes de CRUD se conservan; no se duplican, y los bloques `membership` siguen siendo decorativos.

## 2. Decisiones y compatibilidad

- **No se agregan roles globales:** `admin` existe en `platform_admins`; los roles locales se derivan de `gym_staff_roles` / `gym_members`.
- **Sin registro público:** las altas nuevas requieren JWT y privilegios. Por defecto `USER_API_ENABLED=false` mantiene la decisión de crearlas únicamente por SQL, hasta aprobar activación de las altas guiadas.
- **No crear usuarios a través del POST existente `/members` ni `/staff`:** siguen vinculando `userId` existente. La API nueva `register` es transaccional y opcional.
- **Página pública separada:** `/g/:slug` debe existir en el **router del frontend**, no en el backend. El frontend consume el JSON `/api/v1/public/gyms/{slug}/landing`. Requiere fallback `index.html` del hosting Vue.
- **Versionado de landing:** se añade `V3__landing_publications.sql`; `gym_settings.landing_published` sigue gobernando la visibilidad. Actualizar bloques NO actualiza la versión pública; republicar sí.
- **No se implementó eliminación lógica** (`deleted_at`, `deleted_by`). Los DELETE existentes permanecen con su comportamiento original; Gym con referencias genera conflicto de integridad, sin CASCADE destructivo nuevo.
- **No se modifica V1/V2 ni se hace referencia fija a `test`** desde nuevas consultas.
- **No se implementan pagos electrónicos, planes de landing operativos, refresh tokens ni recuperación de contraseña.**

## 3. Ajustes sobre rutas existentes

1. `POST /gyms`: si omite slug se genera a partir del nombre, verificando unicidad; colisión explícita genera `409 slug_taken`. El admin conserva el PATCH global existente; el owner solo dispone del PATCH acotado al slug.
2. `PATCH /gyms/{gymId}/settings`: si recibe `landingPublished`, rechaza el cambio con 400 para impedir publicar sin generar snapshot; usar POST publish/unpublish.
3. `GET /gyms/{gymId}/classes`: ahora recibe opcionales `from`, `to`, `status`; por defecto lista solo `scheduled`; `status=all` consulta histórico. Se mantienen límites `offset` y `limit`.
4. `MemberRepository.DETAIL`: incluye `m.user_id`, `photo_url` de activos externos y `GET members/me` añade `accessStatus` evaluado con `v_member_access`.
5. `SecurityConfig`: solo `GET /api/v1/public/gyms/*/landing` permite acceso anónimo; no se abren escrituras ni listados privados.
6. `ApiExceptionHandler`: errores tipados `409` y `422` para conflicto/landing no lista.

## 4. Configuración mínima

Añadir variables por entorno (o mantener los defaults de desarrollo):

```properties
SCHEMA_DB=test
PUBLIC_SITE_BASE_URL=http://localhost:5173
# FALSE mantiene el aprovisionamiento manual por SQL aprobado en el README maestro
USER_API_ENABLED=false
FRONTEND_ORIGIN=http://localhost:5173
```

`PUBLIC_SITE_BASE_URL` se utiliza para construir el enlace compartible; en producción debe ser HTTPS con dominio real, sin rutas. Si el panel y la web pública usan dominios distintos, configurar correctamente CORS, CDN y la aplicación Vue.

Para habilitar U01–U06, M01 y S01 **tras revisar la decisión de negocio**, definir `USER_API_ENABLED=true`. Solo admin de plataforma administra cuentas globales; owner registra miembro o trainer para su propio gym. Ninguna operación crea un `platform_admin`.

## 5. Ejemplos de contratos nuevos

**Publicar:** `POST /api/v1/gyms/{gymId}/landing/publish` autenticado con Bearer owner/admin; request `{}` o sin body. Respuesta `{published,slug,publicUrl,version,publishedAt}`.

**Cambiar enlace:** `PATCH /api/v1/gyms/{gymId}/landing/slug` con `{ "slug": "gimnasio-elite" }`.

**Página pública:** `GET /api/v1/public/gyms/gimnasio-elite/landing` sin bearer: `{ gym, theme, blocks, publishedAt, version }`. Al estar despublicada/no disponible devuelve `404`.

**Alta con bandera activa:** `POST /api/v1/gyms/{gymId}/members/register` con `{ "user": { "fullName":"Ana Pérez","email":"ana@example.com","password":"contrasena-fuerte" }, "member": { "code":"M-0001", "active":true } }`. Si existe el email: `409 email_already_exists` y se enlaza mediante POST `/members` ya existente bajo permisos válidos.

**Reservas propias:** `GET /api/v1/gyms/{gymId}/reservations/me?from=2026-11-01&to=2026-11-30&status=booked&offset=0&limit=50`.

**Métricas:** `GET /api/v1/gyms/{gymId}/dashboard/summary?from=2026-11-01&to=2026-11-30`. Los ingresos se separan en `sales.revenue` y `payments.collected`.

## 6. Verificación antes de integrar Vue

- Verificar que el proyecto raíz definitivo tiene **`pom.xml`**, `src`, `.env` correctamente configurado y Maven 3.9+/Java 21.
- Ejecutar `mvn clean test` y revisar que Spring Boot registre todos los repositorios/beans y endpoints sin conflicto; corregir cualquier fallo en ese entorno.
- Aplicar Flyway V1→V3 en un **schema de prueba nuevo** y V3 sobre una base existente V1/V2 sin alterar checksums históricos; revisar que existe configuración de `search_path`.
- Realizar pruebas HTTP con Postman/curl de L01-L05, `/members/me`, `/reservations/me`, disponibilidad, `/checkins/me`, `/dashboard/summary` y bandera de alta deshabilitada.
- Probar owner del gym A contra gym B (incluyendo slug/cuentas) para 403/404; miembro no lee datos de otro; anónimo solo lee publicación activa.
- Publicar, editar borrador, constatar que GET público **no cambia**, republicar, despublicar y verificar 404; confirmar que el enlace nuevo funciona tras cambiar slug y el anterior deja de funcionar.
- Probar base con dos slugs simultáneos, bloqueo real de duplicados y errores 409; comprobar concurrencia de reservas y check-in duplicado.
- Revisar permisos definitivos y seguridad del contenido JSON de bloques; no usar `v-html` en Vue sin sanitización adicional.

**Verificado en este entorno:** parseo de sintaxis de todos los `.java` con JDK 21 (`JavacTask.parse()`): **0 errores**; compilación aislada de la política de slug y casos de normalización con `javac`: correcta. **No verificado:** Maven/JUnit, PostgreSQL, Flyway ejecutado o peticiones HTTP reales. No desplegar a producción sin esos pasos.
