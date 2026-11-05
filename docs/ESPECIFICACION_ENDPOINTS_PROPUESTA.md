> **DECISIÓN ACTUAL (revisión posterior):** la publicación/despublicación se descartó. Este documento conserva la propuesta histórica para consulta; los contratos vigentes están en `docs/DECISION_LANDING_ENLACE_DIRECTO.md`. No implementar L02/L03 ni exigir snapshots.

# GYM BACK — Especificación integral de endpoints pendientes e integración con Vue

**Tipo:** documento funcional y técnico de propuesta (sin implementar).  
**Fecha de análisis:** 3 de noviembre de 2026.  
**Archivos revisados:** `src (2).zip` (frontend Vue 3 / Pinia) y `src.zip` (backend Java / Spring Boot / PostgreSQL).  
**Base documental:** `README_MASTER_BACKEND.md`, `docs/ARQUITECTURA_Y_PATRONES.md`, `docs/CONTRATOS_FRONTEND.md`, `docs/DECISIONES_PENDIENTES.md`, `docs/SEGURIDAD_Y_BD.md`, `docs/VERIFICACION.md`, migraciones Flyway y código de controladores/servicios/repositorios.  
**Prefijo de API:** `/api/v1`. Los ejemplos son **contratos propuestos**, no rutas actualmente implementadas.

> **Importante:** este documento NO modifica código, SQL, rutas, configuración ni archivos de los ZIP. Distingue entre endpoint nuevo, ampliación de uno existente y funcionalidad opcional. No debe tratarse como prueba de que los endpoints ya funcionan.

---

## 0. Alcance, decisiones y criterios

### 0.1 Qué se quiere lograr

1. Completar las funciones reales que el frontend ya presenta: usuarios, miembros, calendario, reservas, acceso, imágenes, personal y dashboard.
2. Permitir que un propietario prepare una página de su gimnasio, **publique/despublique** y comparta un enlace personalizado derivado del nombre: `https://tudominio.com/g/iron-fitness`.
3. Permitir lectura **anónima y de solo contenido público** de esa página usando el `slug`; nunca exponer el editor, usuarios, pagos o datos privados.
4. Respetar la separación actual de capas y el aislamiento entre gimnasios.
5. **No convertir los planes decorativos de la landing en suscripciones operativas ni en una pasarela de pago.**

### 0.2 Advertencias de decisiones ya documentadas en el backend

El documento maestro actual confirma que las cuentas globales `users` se provisionan **manualmente por SQL** y que no hay registro/invitaciones por API. Por tanto, los endpoints de **crear usuarios y registrar personas de cero** de este documento **requieren aprobar un cambio de alcance** y actualizar las guías antes de implementarlos. No deben añadirse silenciosamente.

La matriz definitiva de permisos sigue sin decidirse. Los permisos aquí etiquetados **«propuestos»** son recomendaciones a aprobar, no permisos ya concedidos en producción. Se mantienen como hechos las autorizaciones actualmente codificadas donde se indica **«hoy»**.

Otras exclusiones vigentes: pagos online, pasarela, reembolsos, conciliación, refresh tokens y recuperación de contraseña; no se presentan como necesarios para la integración inicial. No inventar un rol global `owner`, `trainer` o `member`: son relaciones por gimnasio.

### 0.3 Convenciones

- `{gymId}`, `{id}`, `{memberId}`, `{deviceId}`: UUID.
- `{slug}`: identificador legible, sin UUID, estable y **único globalmente**.
- **P0:** necesario para la publicación de páginas y/o operación central. **P1:** necesario para cerrar flujos del frontend. **P2:** mejora operativa. **Futuro:** condicionado a activar funcionalidad hoy informativa o incompleta.
- **Nuevo:** no hay controlador para la ruta. **Ajuste:** existe ruta pero el comportamiento/contrato es incompleto. **Existente:** no duplicar.
- Los ejemplos usan nombres `camelCase` en contratos nuevos. Las respuestas SQL crudas existentes en `snake_case` requieren mapeo/normalización explícita: no cambiar accidentalmente los contratos ya usados.
- El actor siempre se extrae del token o de un mecanismo autenticado del dispositivo, **nunca** de un `actorUserId` enviado por Vue.
- El esquema real proviene de `schema_db`/`SCHEMA_DB`; **no escribir `test` de forma fija** en Java/migraciones (aunque el esquema del entorno local pueda ser `test`).

---

## 1. Inventario verificado de API EXISTENTE: NO DUPLICAR

| Área | API existente | Hecho comprobado / limitación |
|---|---|---|
| Auth | `POST /auth/login`, `GET /auth/me` | JWT, `user` y `gyms[]` con `roles[]`; selector después de autenticar. |
| Gimnasios | `GET,POST /gyms`; `GET,PATCH,DELETE /gyms/{id}` | Solo admin de plataforma en `GymService`; `DELETE` usa `hardDelete` y puede chocar con FKs. |
| Staff | `GET,POST /gyms/{gymId}/staff`; `GET,PATCH,DELETE .../staff/{id}` | Vincula `userId` existente; permisos y multirrol limitados. |
| Miembros | `GET,POST /gyms/{gymId}/members`; `GET,PATCH,DELETE .../members/{id}` | Vincula cuenta existente; DELETE desactiva miembro. |
| Planes operativos | CRUD `/gyms/{gymId}/plans` | Son los que utiliza el cobro administrativo de membresías. |
| Pagos | `GET,POST /gyms/{gymId}/payments` | Pago manual, NO cargo bancario. |
| Clases/plantillas | CRUD `/gyms/{gymId}/classes`, `/class-templates` | Categorías incluyen `functional` con migración V2. |
| Reservas | `GET,POST /gyms/{gymId}/reservations`, `DELETE /reservations/{id}` | GET requiere `classId` y staff; POST y cancelar permiten miembro. |
| Rutinas | CRUD `/gyms/{gymId}/routines` | Miembro solamente ve las publicadas. |
| Productos/ventas | CRUD `/products`, `GET,POST /sales` | Ventas calculadas por backend; sin confiar en precios enviados. |
| Check-in | `GET,POST /gyms/{gymId}/checkins` | Auditoría y entrada manual administrativa, no flujo QR real. |
| Configuración | `GET,PATCH /gyms/{gymId}/settings` | **Ya acepta `landingPublished`** en `SettingsPatch`; no equivale a un flujo editorial completo. |
| Módulos | `GET /gyms/{gymId}/modules`, `PATCH /modules/{key}` | Cuatro módulos configurables. |
| Landing/editor | CRUD `/gyms/{gymId}/landing-blocks` | Solo autenticado; los bloques se guardan en `blockData` JSON. |
| Multimedia | CRUD `/gyms/{gymId}/media-assets` | Solo registra referencias URL HTTPS; no sube binarios. |

**Hechos de base de datos que deben aprovecharse:**

- `gym.name`, `gym.short_name`, `gym.slug` (`CITEXT UNIQUE`), `gym.status`.
- `gym_settings.landing_published` (`BOOLEAN DEFAULT FALSE`). Se crea mediante `trg_initialize_gym_defaults` cuando se inserta un gimnasio.
- `landing_blocks` tiene `gym_id`, `enabled`, `sort_order`, `block_type`, `block_data` JSONB.
- `media_assets` ya tiene `storage_key` y `external_url` (exactamente uno de los dos).
- Para QR ya existen `checkin_devices`, `checkin_qr_tokens`, `checkin_events`.
- Para invitados ya existe `guest_passes`, aunque falta flujo de API.

---

## 2. Mapa ejecutivo de endpoints NUEVOS propuestos

> Las rutas de la tabla son **propuestas**. `/api/v1` se omite dentro de las celdas para ahorrar espacio.

| ID | Prioridad | Método y ruta | Pantalla/consumidor | Objetivo |
|---|---|---|---|---|
| U01 | P1, **cambio de decisión** | `POST /users` | `UsersPage.vue`, administración | Crear una cuenta global. |
| U02 | P1, **cambio de decisión** | `GET /users` | `UsersPage.vue` para admin | Enumerar cuentas con paginación. |
| U03 | P1, **cambio de decisión** | `GET /users/{id}` | `UsersPage.vue` | Consultar una cuenta sin secretos. |
| U04 | P1, **cambio de decisión** | `PATCH /users/{id}` | `UsersPage.vue` | Editar datos globales, no asignaciones de gym. |
| U05 | P1, **cambio de decisión** | `PATCH /users/{id}/status` | `UsersPage.vue` | Habilitar/deshabilitar cuenta global. |
| U06 | P1, **cambio de decisión** | `GET /users/search?email=...` | Registro de miembro/staff | Localizar cuenta previamente creada, con permiso. |
| M01 | P1, **cambio de decisión** | `POST /gyms/{gymId}/members/register` | `MemberRegistrationForm.vue` | Crear cuenta nueva y expediente en una transacción. |
| M02 | P1 | `GET /gyms/{gymId}/members/me` | `CheckInPage.vue`, perfil de miembro | Obtener solo expediente del usuario autenticado. |
| S01 | P2, **cambio de decisión** | `POST /gyms/{gymId}/staff/register` | `UsersPage.vue` del dueño | Alta guiada de entrenador nuevo, opcional frente a U01 + staff existente. |
| R01 | P1 | `GET /gyms/{gymId}/reservations/me` | `CalendarPage.vue`, `DayScheduleModal.vue` | Ver reservas propias. |
| R02 | P1 | `GET /gyms/{gymId}/classes/{classId}/availability` | Calendario de miembro | Mostrar cupos y estado propio sin exponer asistentes. |
| C01 | P1 | `GET /gyms/{gymId}/checkins/me` | `CheckInPage.vue` | Historial de acceso del propio miembro. |
| L01 | P0 | `GET /gyms/{gymId}/landing/publication` | `LandingBuilderPage.vue` | Estado, slug y enlace que puede compartirse. |
| L02 | P0 | `POST /gyms/{gymId}/landing/publish` | Botón **Publicar** | Publicar una versión validada de la landing. |
| L03 | P0 | `POST /gyms/{gymId}/landing/unpublish` | Botón **Despublicar** | Retirar acceso público sin borrar contenido. |
| L04 | P0 | `PATCH /gyms/{gymId}/landing/slug` | Configuración de link | Personalizar URL de la página del gimnasio. |
| L05 | P0 | `GET /public/gyms/{slug}/landing` | `HomePage.vue` en `/g/:slug`; visitantes | Obtener contenido publicado sin autenticación. |
| F01 | P1 | `POST /gyms/{gymId}/media-assets/upload` | `MediaUploader.vue`, foto de miembro | Subir archivo al almacenamiento. |
| Q01 | P1 si se habilita QR | `GET /gyms/{gymId}/checkin-devices` | Configuración de kioscos | Listar dispositivos autorizados. |
| Q02 | P1 si se habilita QR | `POST /gyms/{gymId}/checkin-devices` | Configuración de kioscos | Dar de alta dispositivo de acceso. |
| Q03 | P1 si se habilita QR | `PATCH /gyms/{gymId}/checkin-devices/{deviceId}` | Configuración de kioscos | Desactivar/renombrar dispositivo. |
| Q04 | P1 si se habilita QR | `POST /kiosks/{deviceId}/qr-token` | `StandaloneCheckInPage.vue` en dispositivo emparejado | Emitir QR temporal. |
| Q05 | P1 si se habilita QR | `POST /gyms/{gymId}/checkins/validate` | `CheckInPage.vue` del miembro | Validar QR de kiosco con JWT del miembro. |
| Q06 | P1 si se habilita QR | `POST /kiosks/{deviceId}/checkins` | Kiosco emparejado | Validar código introducido en kiosco. |
| D01 | P2 | `GET /gyms/{gymId}/dashboard/summary` | `DashboardPage.vue` | Estadísticas completas por rango, sin recortes por límite de listados. |
| G01 | Futuro condicionado | `POST /gyms/{gymId}/guest-passes` | Personal del gimnasio | Emitir pase temporal de invitado. |
| G02 | Futuro condicionado | `GET /gyms/{gymId}/guest-passes` | Administración de invitados | Consultar pases. |
| G03 | Futuro condicionado | `POST /gyms/{gymId}/guest-passes/{id}/revoke` | Administración de invitados | Revocar pase sin borrar su historial. |

**Total: 28 endpoints nuevos propuestos.** Desglose: usuarios U01–U06 (6), miembros M01–M02 (2), staff S01 (1), reservas R01–R02 (2), historial de check-in C01 (1), landing L01–L05 (5), multimedia F01 (1), QR/dispositivos Q01–Q06 (6), dashboard D01 (1) e invitados G01–G03 (3). Los de invitados y el alta guiada de staff son condicionales y no deben bloquear la primera entrega.

---

## 3. Landing pública: publicación, despublicación y enlace personalizado (P0)

### 3.1 Resultado esperado para un propietario y para un visitante

**Propietario:** entra a `Dashboard > Landing Builder`, edita bloques/tema/medios, ve vista previa privada, personaliza el slug y pulsa **Publicar**. La API responde con el enlace para copiar y compartir. Si modifica borradores, el sitio ya publicado **no cambia** hasta que vuelva a publicar. Si pulsa **Despublicar**, el contenido sigue guardado pero deja de ser accesible para visitantes.

**Visitante:** abre `https://tudominio.com/g/iron-fitness` sin JWT; Vue interpreta `/g/:slug`, invoca `GET /api/v1/public/gyms/iron-fitness/landing` y renderiza exclusivamente la versión pública.

**No confundir:** la URL visible que se comparte (`/g/:slug`) es una **ruta del frontend**, mientras que `/api/v1/public/gyms/{slug}/landing` es la **API JSON**. Un endpoint JSON por sí solo **no convierte** una URL en página HTML; el frontend necesita el router `/g/:slug` y la infraestructura web debe reescribir esa ruta a `index.html` de Vue.

### 3.2 Política de slug basada en nombre

- Al **crear** un gimnasio con el `POST /gyms` existente, proponer generar `slug` desde `name` si el administrador no lo suministra: `Iron Fitness` → `iron-fitness`; `Gimnasio Élite` → `gimnasio-elite`.
- Normalizar Unicode, quitar acentos, minúsculas, reemplazar espacios/caracteres incompatibles por guion, colapsar guiones, quitar guiones de bordes y validar longitud **3–80** como exige hoy `GymService`.
- Respetar `gym.slug` como `CITEXT UNIQUE`, de modo que `Iron-Fitness` y `iron-fitness` no se dupliquen. Para conflictos automáticos, puede intentarse `iron-fitness-2` con reintento transaccional; para un slug elegido explícitamente, devolver **409 `slug_taken`** y no cambiarlo silenciosamente.
- Reservar rutas/identificadores técnicos (por ejemplo `admin`, `api`, `auth`, `login`, `dashboard`, `checkin`, `g`) en una lista del Service, aunque el enlace use el prefijo `/g/`.
- Una vez creado, el slug es **independiente** de los cambios posteriores de nombre; editar el nombre no rompe enlaces compartidos.
- Si se autoriza cambiar el slug publicado, **el enlace anterior dejará de funcionar** salvo que se introduzcan alias y redirecciones. Para la primera entrega: advertir esto en UI y exigir confirmación; opcionalmente añadir `gym_slug_aliases` en una migración posterior y redirigir `/g/antiguo` a `/g/nuevo`.
- `PUBLIC_SITE_BASE_URL` debe configurarse por ambiente (por ejemplo `https://tudominio.com`), sin concatenar un dominio fijo en código Java. Nunca construir URLs desde un `Host` HTTP no confiable.

### 3.3 L01 — Consultar estado y enlace

**`GET /api/v1/gyms/{gymId}/landing/publication`** — **Nuevo, P0**.

- **Objetivo:** mostrar en `LandingBuilderPage.vue` si está publicada, slug actual, URL para compartir, última publicación y versión activa.
- **Permisos propuestos:** `admin` global, `owner` del gimnasio. `trainer` solo si se aprueba permiso expreso de visualización.
- **Service:** valida que el actor tiene acceso a ese `gymId`; carga `gym` + `gym_settings` + publicación/snapshot.
- **Respuesta 200:**

```json
{
  "gymId": "00000000-0000-0000-0000-000000000010",
  "gymName": "Iron Fitness",
  "slug": "iron-fitness",
  "published": true,
  "publicUrl": "https://tudominio.com/g/iron-fitness",
  "publishedAt": "2026-11-03T18:00:00Z",
  "publishedVersion": 3,
  "hasUnpublishedChanges": true
}
```

- **Al no estar publicada:** `published=false`, `publicUrl=null` (o un `shareable=false` inequívoco), `publishedAt=null` si nunca se publicó. No prometer a los visitantes una URL navegable como si ya estuviera pública.
- **Códigos:** 200; 401 sin JWT; 403 sin permiso/gimnasio suspendido según política; 404 si no existe.
- **Persistencia:** consulta, no cambia datos.

### 3.4 L02 — Publicar (o volver a publicar)

**`POST /api/v1/gyms/{gymId}/landing/publish`** — **Nuevo, P0**.

- **Objetivo:** convertir el borrador actual en la versión pública oficial. También sirve para **republicar** cambios.
- **Pantalla:** botón **Publicar cambios** de `LandingBuilderPage.vue`.
- **Permisos propuestos:** admin global o `owner` del gimnasio **activo**. No `member`, `guest` ni anónimo.
- **Request:** `{}` (no se recibe `gymId`, `slug` ni `publishedBy` del body). Opcionalmente `expectedDraftRevision` para control optimista de concurrencia cuando ese número esté disponible.
- **Comportamiento correcto, transaccional:** 
  1. Comprobar actor, `gym.status='active'`, `gym.slug` válido/único, configuración y al menos un bloque publicable habilitado; validar tipos conocidos, orden, esquema de `blockData`, URLs y medios referenciados.
  2. Reunir `gym` público (`name`, `shortName`, `slug`, `logoUrl`), aspecto público (`theme_preset`, `theme_overrides`) y bloques habilitados y ordenados. Excluir campos internos o datos de personas.
  3. **Guardar un snapshot público inmutable** de esa versión y aumentar `version`; `landing_published=true`. No depender en cada visita del contenido editable en vivo.
  4. Registrar `publishedBy` de JWT, fecha del servidor y, si se habilita, evento en `audit_events`.
  5. Invalidar caché de la URL pública y devolver el link.
- **Respuesta 200** si publicada/republicada; no hace falta crear una landing diferente cada vez:

```json
{
  "published": true,
  "slug": "iron-fitness",
  "publicUrl": "https://tudominio.com/g/iron-fitness",
  "version": 4,
  "publishedAt": "2026-11-03T18:10:00Z"
}
```

- **Errores:** 400 formato inválido, 401/403 auth, 404 gym inexistente, 409 versión de borrador obsoleta si aplica, 422 `landing_not_ready` si faltan contenidos imprescindibles. El formato actual de `ApiExceptionHandler` mapea `DomainException` a 400; se recomienda ampliar su taxonomía antes de usar 422.
- **Garantía:** si algo falla, rollback **sin** activar la publicación ni sustituir el snapshot anterior.

### 3.5 L03 — Despublicar

**`POST /api/v1/gyms/{gymId}/landing/unpublish`** — **Nuevo, P0**.

- **Objetivo:** dejar de compartir la página, **sin borrar** bloques, temas, fotos ni historial de publicación.
- **Pantalla:** botón **Despublicar** (confirmación visual en Vue).
- **Permisos propuestos:** admin global o `owner` del gimnasio.
- **Request:** `{}`.
- **Comportamiento:** marcar `landing_published=false` de forma idempotente; conservar el último snapshot para posible republicación; invalidar caché; auditar si se implementó auditoría. El usuario autenticado puede seguir editando y previsualizando.
- **Respuesta 200:** `{ "published": false, "publicUrl": null }`.
- **Efecto público:** desde que se aplica, `GET /public/gyms/{slug}/landing` devuelve **404**, aunque existiera antes; evitar entregar una copia obsoleta por caché.

### 3.6 L04 — Personalizar la URL

**`PATCH /api/v1/gyms/{gymId}/landing/slug`** — **Nuevo, P0**.

- **Objetivo:** permitir al dueño elegir un enlace fácil de compartir basado inicialmente en el nombre.
- **Pantalla:** campo **Enlace personalizado** con previsualización y botón Guardar en el builder.
- **Permisos propuestos:** admin global o `owner` de **ese** gimnasio. Es una excepción controlada a `GymService.change()`, que hoy solo deja editar gimnasio al admin global; NO abrir todo `PATCH /gyms/{id}` a propietarios.
- **Request:** `{ "slug": "iron-fitness-centro" }`.
- **Comportamiento:** normalizar y validar conforme a la política, comprobar unicidad real en DB, actualizar **solo `gym.slug`**, auditar, actualizar vínculo visible. Un cambio de slug no publica una página despublicada.
- **Respuesta 200:** `{ "gymId": "...", "slug": "iron-fitness-centro", "published": true, "publicUrl": "https://tudominio.com/g/iron-fitness-centro" }`.
- **Conflictos:** 400 slug inválido/reservado, **409** si ya lo usa otro gimnasio. Cambios en `gym.name` no reescriben automáticamente el slug. Si snapshot publicó el slug anterior, al cambiar se debe actualizar su metadato/lookup de manera consistente, **sin filtrar borradores**.
- **Permisos:** un propietario del Gym A jamás puede cambiar el slug del Gym B aunque adivine su UUID.

### 3.7 L05 — Consumir la página públicamente

**`GET /api/v1/public/gyms/{slug}/landing`** — **Nuevo, P0, sin JWT**.

- **Objetivo:** entregar contenido público JSON que puede consultar **cualquier visitante** y consumir `HomePage.vue` al navegar a `/g/:slug`.
- **Permisos:** anónimo; excepción explícita `GET` en `SecurityConfig`. No reutilizar `AuthorizationService.actor()` porque requiere JWT; usar un Service de consulta pública que **no acepte** credenciales como condición para esta lectura.
- **Comportamiento:** buscar `gym.slug` de forma insensible a mayúsculas, comprobar `gym.status='active'` y `gym_settings.landing_published=true`, obtener el **snapshot aprobado**. No leer directamente los últimos bloques de edición para mostrar borradores.
- **Respuesta 200 de ejemplo:**

```json
{
  "gym": {
    "name": "Iron Fitness",
    "shortName": "Iron",
    "slug": "iron-fitness",
    "logoUrl": "https://cdn.ejemplo.com/iron/logo.png"
  },
  "theme": {
    "preset": "crossfit-dark",
    "overrides": {}
  },
  "blocks": [
    {
      "id": "00000000-0000-0000-0000-000000000101",
      "type": "hero",
      "order": 0,
      "enabled": true,
      "data": { "title": "Entrena con nosotros", "subtitle": "Conoce el gimnasio" }
    },
    {
      "id": "00000000-0000-0000-0000-000000000102",
      "type": "membership",
      "order": 1,
      "enabled": true,
      "data": {
        "title": "Nuestros planes",
        "plans": [{ "name": "Mensual", "price": "$599", "benefits": ["Acceso general"] }]
      }
    }
  ],
  "publishedAt": "2026-11-03T18:10:00Z",
  "version": 4
}
```

**Muy importante:** el bloque `membership` anterior usa `landing_blocks.block_data.plans` **solo como texto informativo**. **NO** se consulta automáticamente `membership_plans`, no se activa una suscripción, no se paga ni se crea usuario al pulsar un bloque decorativo.

- **Respuesta 404 en todos estos casos:** slug inexistente, gimnasio suspendido/archivado, página no publicada, snapshot no disponible. Responder igual para no permitir enumerar gimnasios privados.
- **Seguridad:** lista blanca de campos, sanitizar URLs/contenido renderizable, bloquear scripts HTML peligrosos, no retornar `gym_settings` privado entero, `media_assets` internos, usuarios, listas de miembros, check-ins, pagos ni datos de auditoría.
- **Operación:** `Cache-Control` apropiado y purga en publish/unpublish; limitación de tasa y métricas de errores; CDN solo sobre recursos públicos.

### 3.8 Persistencia recomendada para un verdadero borrador vs. publicado

**Opción mínima, sin tabla nueva:** los cambios sobre `landing_blocks` aparecen públicamente en tiempo real mientras `landing_published=true`. Es técnicamente sencilla, pero **NO** satisface la expectativa de «editar sin publicar cambios». Si se elige, debe indicarse explícitamente en la interfaz.

**Opción recomendada para este proyecto:** una **migración Flyway aditiva** con tabla `landing_publications`, por ejemplo:

```sql
-- ESQUEMA PROPUESTO, NO EJECUTAR SIN REVISIÓN.
-- Usar la siguiente versión Flyway libre; no cambiar V1/V2 aplicadas.
CREATE TABLE ${schema_db}.landing_publications (
    gym_id UUID PRIMARY KEY REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    snapshot JSONB NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);
```

`gym_settings.landing_published` **sigue siendo la fuente de verdad del estado**, para aprovechar lo que ya existe; `landing_publications` guarda **la última versión publicada**, no reemplaza `landing_blocks`. Ambos cambios de publish se realizan en la misma transacción.

`hasUnpublishedChanges` requiere comparación fiable: por ejemplo almacenar `draft_checksum`/`published_checksum`, o una revisión del borrador calculada desde bloques/tema; no devolver valores ficticios si no existe ese mecanismo.

### 3.9 Ejemplo de flujo completo

```text
ADMIN crea Gym "Iron Fitness"
  -> POST /api/v1/gyms
  -> gym.slug = iron-fitness (si se aprueba autogeneración)
  -> gym_settings.landing_published = false (ya existe trigger SQL)

OWNER edita bloques y tema existentes (CRUD autenticado)
  -> GET /api/v1/gyms/{gymId}/landing/publication
  -> POST /api/v1/gyms/{gymId}/landing/publish
  <- publicUrl: https://tudominio.com/g/iron-fitness

VISITANTE abre https://tudominio.com/g/iron-fitness
  -> Vue ruta /g/:slug
  -> GET /api/v1/public/gyms/iron-fitness/landing (sin bearer)
  <- JSON público con gym, theme, blocks

OWNER edita de nuevo (solo borrador)
  -> público sigue con la última versión aprobada
  -> POST /api/v1/gyms/{gymId}/landing/publish
  -> público recibe nueva versión

OWNER despublica
  -> POST /api/v1/gyms/{gymId}/landing/unpublish
  -> GET público devuelve 404; el borrador se conserva
```

---

## 4. Cuentas globales y relación con gimnasios (P1; requiere cambiar decisión de SQL)

### Regla central de identidad

- `users` representa una **persona global**; nunca insertar cuentas duplicadas por cada gimnasio.
- `platform_admins` representa privilegio global técnico.
- `gym_staff_roles` representa roles locales `owner`/`trainer` (puede haber varios roles en un mismo gimnasio).
- `gym_members` representa la relación local y expediente `member`.
- **No** crear un campo simplificador `users.role` ni confiar en un rol enviado desde Vue.
- Hasta que se apruebe la API de cuentas, **conservar el aprovisionamiento SQL actual**; ninguna ruta de esta sección está disponible hoy.

### U01 — Crear usuario global

**`POST /api/v1/users`** — **Nuevo, P1, cambio de alcance**.

- **Objetivo:** dejar de registrar personas manualmente por SQL; la creación global debe ser independiente del rol o gimnasio.
- **Pantalla:** `UsersPage.vue` en contexto de administrador técnico.
- **Permiso propuesto:** **solo admin global**. Para un dueño que necesite registrar miembro/entrenador, preferir M01/S01 de alcance limitado, no abrir libremente esta ruta.
- **Request ejemplo:**

```json
{
  "fullName": "Ana Pérez",
  "email": "ana@ejemplo.com",
  "phone": "+52 777 000 0000",
  "password": "contrasena-fuerte",
  "status": "active"
}
```

- **Comportamiento:** normalizar email, validar unicidad `users.email` CITEXT, aplicar BCrypt del `PasswordEncoder` configurado, nunca guardar contraseña plana ni devolver hash. Si se adopta invitación con contraseña diferida, definirla como otro alcance; **no** dejar cuentas activas sin contraseña.
- **Respuesta:** `201 Created`, `{ "id": "UUID", "fullName": "Ana Pérez", "email": "ana@ejemplo.com", "phone": "...", "status": "active" }`, `Location: /api/v1/users/{id}`.
- **Errores:** 400 validación; 409 email ocupado; 401/403 permisos.
- **Tablas:** `users`. No crea automáticamente `gym_staff_roles` ni `gym_members`.

### U02 — Listar usuarios globales

**`GET /api/v1/users?offset=0&limit=50&query=ana&status=active`** — **Nuevo, P1**.

- **Pantalla:** `UsersPage.vue` del admin.
- **Permiso propuesto:** admin global; los owners no deben enumerar todos los usuarios del sistema.
- **Comportamiento:** paginar en DB, filtros parametrizados, búsqueda por nombre/email, `limit` máximo 100, orden estable.
- **Respuesta sugerida:** `{ "items": [{"id":"...","fullName":"Ana Pérez","email":"...","status":"active"}], "total": 1, "offset": 0, "limit": 50 }`. Nunca incluir `passwordHash`.

### U03 — Consultar cuenta global

**`GET /api/v1/users/{id}`** — **Nuevo, P1**.

- **Pantalla:** detalle/edición de `UsersPage.vue`.
- **Permiso propuesto:** admin global. El propio usuario usa `/auth/me` para sesión; si se desea edición de perfil propia, se diseña separadamente para no abrir datos globales.
- **Comportamiento:** devuelve datos editables y, para admin, resumen de asignaciones por gimnasio si está justificado; nunca credenciales ni secretos.
- **Respuesta:** 200; 404 si no existe.

### U04 — Actualizar datos globales

**`PATCH /api/v1/users/{id}`** — **Nuevo, P1**.

- **Pantalla:** edición de usuarios en `UsersPage.vue`.
- **Permiso propuesto:** admin global.
- **Request:** `{ "fullName": "Ana María Pérez", "phone": "+52...", "email": "nuevo@ejemplo.com" }`.
- **Comportamiento:** lista blanca; nunca cambiar `id`, `password_hash`, `platformAdmin`, `gymIds` ni roles por este PATCH. Validar unicidad de email; si se cambia email, decidir política de reverificación. `updated_at` se actualiza.
- **Respuesta:** 200 con perfil editado; 409 por email repetido.

### U05 — Habilitar o deshabilitar cuenta global

**`PATCH /api/v1/users/{id}/status`** — **Nuevo, P1**.

- **Pantalla:** acción activar/desactivar en `UsersPage.vue`.
- **Permiso propuesto:** admin global.
- **Request:** `{ "status": "disabled" }` o `{ "status": "active" }`.
- **Comportamiento:** deshabilitar impide nuevos logins y el `AuthorizationService.actor()` ya debe rechazar el usuario en las solicitudes siguientes. Proteger al último admin de plataforma y la autodesactivación accidental; no borrar ventas, membresías, rutinas ni historial.
- **Respuesta:** 200 con estado actual. No convertir `DELETE /users` en borrado físico de datos con referencias.

### U06 — Buscar una cuenta para asociarla

**`GET /api/v1/users/search?email=ana%40ejemplo.com`** — **Nuevo, P1**.

- **Pantalla:** selectores de `MemberRegistrationForm.vue` y `UsersPage.vue`.
- **Permisos propuestos:** admin global o propietario con una operación de asociación explícita en su gimnasio; prohibido para anónimo.
- **Comportamiento:** búsqueda exacta normalizada (no enumeración abierta), respuesta mínima `{id, fullName, email, status}` cuando la política lo permita. Aplicar límites de tasa y registrar consultas si muestran datos personales. Un owner no debe obtener una lista indiscriminada de cuentas de otros gimnasios.
- **Conflictos de privacidad:** si no se aprueba búsqueda entre tenants, reemplazar por un flujo de invitación/consentimiento y mantener solo U06 para admin.

### M01 — Crear cuenta y expediente de miembro en una transacción

**`POST /api/v1/gyms/{gymId}/members/register`** — **Nuevo, P1, cambio de alcance**.

- **Pantalla:** `MemberRegistrationForm.vue` / `MembersPage.vue`.
- **Permiso propuesto:** admin global o `owner` del gimnasio. Determinar si `trainer` puede dar de alta según matriz definitiva.
- **Request de referencia:**

```json
{
  "user": {
    "fullName": "Carlos López",
    "email": "carlos@ejemplo.com",
    "phone": "+52 777 000 0000",
    "password": "contrasena-fuerte"
  },
  "member": {
    "code": "M-00025",
    "birthDate": "1999-05-10",
    "address": "Cuernavaca",
    "emergencyContactName": "Contacto",
    "emergencyContactPhone": "+52 777 000 0011",
    "photoAssetId": null,
    "active": true
  }
}
```

- **Comportamiento:** validar `gymId` y permiso; comprobar email único; crear `users` con password hash; crear `gym_members` con `gym_id`, `user_id` recién creado y código único por gimnasio; establecer gimnasio por defecto solo si falta; rollback íntegro si falla cualquier paso. No crear automáticamente pago ni activar membresía operativa sin registrar un pago por la API administrativa existente.
- **Respuesta:** `201`, `{ "user": {"id":"...","fullName":"...","email":"..."}, "member": {"id":"...","gymId":"...","userId":"...","code":"M-00025","active":true} }`.
- **Correo existente:** **409 `email_already_exists`** con instrucciones para asociar una cuenta existente mediante el **POST `/members` actual** y su `userId` validado. No enlazar a alguien únicamente por conocer su correo: requiere autorización/consentimiento o un proceso de identidad aprobado.
- **No duplicar:** el endpoint existente `POST /members` seguirá siendo el vínculo de cuenta **ya existente**.
- **Tablas:** `users`, `gym_members`. `media_assets` solo si ya se subió una foto por F01.

### M02 — Miembro autenticado consulta su propio expediente

**`GET /api/v1/gyms/{gymId}/members/me`** — **Nuevo, P1**.

- **Pantallas:** `CheckInPage.vue`, perfil de miembro, resumen de acceso.
- **Permisos:** JWT válido y pertenencia activa al gimnasio; si el usuario también es staff, devolver su expediente de miembro solo si efectivamente existe.
- **Comportamiento:** obtener `userId` del JWT, buscar `gym_members` por `(gym_id, user_id)`; incluir estado de acceso calculado por la regla vigente (vista `v_member_access`), foto pública/resuelta si existe, datos propios y vencimiento. No hacer `GET /members` de todo el gimnasio desde la sesión member.
- **Respuesta ejemplo:** `{ "id":"...", "userId":"...", "gymId":"...", "code":"M-00025", "active":true, "accessStatus":"allowed", "nextPaymentDate":"2026-12-03", "photoUrl":null }`.
- **404:** usuario sin expediente en ese gym; **403:** cuenta sin derecho de acceso al gym.

### S01 — Alta guiada de entrenador nuevo (condicional)

**`POST /api/v1/gyms/{gymId}/staff/register`** — **Nuevo, P2 y cambio de alcance**.

- **Pantalla:** `UsersPage.vue` cuando el dueño quiera registrar un entrenador sin conocer un UUID ni tener acceso administrativo global.
- **Permiso propuesto:** `owner` del gimnasio y admin global. Un `owner` ordinario **solo** puede crear/vincular `trainer`; la creación de `owner` de otro gimnasio queda en política de admin.
- **Request:** `{ "user": {"fullName":"María Díaz","email":"maria@ejemplo.com","password":"..."}, "role":"trainer" }`.
- **Comportamiento:** transacción `users` + `gym_staff_roles` con `role=trainer`, sin conceder acceso a otros gimnasios; cuentas ya existentes se vinculan por `POST /staff` existente bajo un procedimiento autorizado. No insertar rol global ficticio.
- **Respuesta:** `201` con usuario sin secretos y roles locales actualizados.
- **Alternativa sin ruta nueva:** admin crea en U01 y owner/admin usa `POST /staff`. Implementar S01 solo si se decide apoyar explícitamente el flujo de un paso del dueño.

---

## 5. Calendario y reservas propias (P1)

### R01 — Mis reservas

**`GET /api/v1/gyms/{gymId}/reservations/me?from=2026-11-01&to=2026-11-30&status=booked`** — **Nuevo**.

- **Pantallas:** `CalendarPage.vue`, `DayScheduleModal.vue`, perfil de miembro.
- **Permiso:** miembro/staff autenticado y con acceso al gimnasio. **No** aceptar un `userId` arbitrario desde cliente; obtener `actor.id()`.
- **Comportamiento:** filtrar `gym_id`, `user_id` autenticado, rango de fechas y estado; paginar; ordenar por inicio. Incluir clase, horario y estado de reserva. Nunca mostrar reservas ajenas.
- **Respuesta:** `{ "items": [{"id":"...","classId":"...","status":"booked","classStartsAt":"2026-11-12T18:00:00-06:00"}], "total":1 }`.
- **Uso:** permitir indicar «Ya reservado» y la cancelación vía **DELETE `/reservations/{id}` existente**.

### R02 — Cupos y estado de disponibilidad de clase

**`GET /api/v1/gyms/{gymId}/classes/{classId}/availability`** — **Nuevo**.

- **Pantallas:** `DayScheduleModal.vue`, `ClassDetailsModal.vue`.
- **Permisos:** miembro/staff autenticado con módulo `calendar` habilitado. Público solo si se decide añadir calendario público y con un contrato separado sanitizado.
- **Comportamiento:** leer `capacity`, contar `booked`/`attended`, calcular plazas restantes, incluir si el usuario tiene reserva y si la clase está cancelada/pasada. No exponer nombres ni emails de asistentes.
- **Respuesta:** `{ "classId":"...", "capacity":20, "reserved":14, "available":6, "bookable":true, "myReservationStatus":null }`.
- **Concurrencia:** el número mostrado es informativo; `POST /reservations` **debe** seguir validando cupos bajo lock en la transacción.

### Correcciones de rutas existentes relacionadas

- **Ajustar** `GET /gyms/{gymId}/classes`: filtros opcionales `from`, `to`, `status` para calendario; respetar paginación y timezone del gimnasio. No crear otro GET si basta ampliar el actual.
- **Ajustar** `ReservationService.book()`: política explícita de solapamientos horarios entre clases; hoy revisa cupo, duplicado y límite de clase regular por día, pero no un conflicto temporal general. Decisión de negocio requerida.
- **Ajustar** `GET /reservations?classId=...`: mantener vista administrativa para owner/trainer, no abrir lista de asistentes a un miembro.
- **Ajustar** cancelación en `GymClassService` y las listas: clases `cancelled` deben mostrarse/ocultarse según filtro y no quedar reservables.

---

## 6. Check-in personal y QR/kiosco (P1 cuando se active)

### C01 — Mis accesos

**`GET /api/v1/gyms/{gymId}/checkins/me?from=2026-11-01&to=2026-11-30&limit=50`** — **Nuevo**.

- **Pantalla:** `CheckInPage.vue` historial del miembro.
- **Permiso:** JWT con perfil del propio miembro en el gimnasio, módulo `checkin` habilitado.
- **Comportamiento:** identificar miembro por `(gym_id, user_id=actor)`, consultar eventos propios, paginar, ordenar y devolver fecha local del gimnasio y resultado (`allowed`, `reason`, `method`). No permitir pasar `memberId` ajeno.
- **Respuesta:** `{ "items": [{"eventId":"...","occurredAt":"2026-11-03T18:00:00-06:00","visitDate":"2026-11-03","allowed":true,"method":"kiosk_qr","reason":"granted"}] }`.

### Distinción: quién muestra QR y quién lo lee

**Flujo A (recomendado):** el kiosco autorizado muestra QR con token corto; el miembro usa la cámara de su móvil autenticado y envía token a Q05.  
**Flujo B:** el miembro introduce su código en un kiosco **emparejado y autenticado**, que lo envía a Q06.  
**No:** un navegador público abierto en `/checkin` no debe tener una contraseña de dispositivo incrustada en JavaScript ni estar autorizado automáticamente a registrar accesos.

### Q01 — Consultar kioscos del gimnasio

**`GET /api/v1/gyms/{gymId}/checkin-devices`** — **Nuevo**.

- **Pantalla:** configuración administrativa de acceso (a añadir al área de check-in).
- **Permisos propuestos:** admin/owner, módulo check-in. Lista `{id, deviceName, active, lastSeenAt}` **sin secretos**.

### Q02 — Registrar o emparejar kiosco

**`POST /api/v1/gyms/{gymId}/checkin-devices`** — **Nuevo**.

- **Objetivo:** crear identidad de dispositivo en `checkin_devices`; el emparejamiento debe utilizar canal protegido, credencial por dispositivo y mecanismo de rotación/revocación.
- **Pantalla:** configuración del owner; el kiosco recibe credenciales **solo durante el emparejamiento seguro**, nunca en la respuesta de un listado.
- **Request:** `{ "deviceName": "Recepción principal" }`.
- **Respuesta:** `201` con `deviceId`, `deviceName`, `active` y material **de emparejamiento de uso limitado** si se define ese mecanismo. Hash de secreto almacenado, nunca texto plano en DB.
- **No permitir:** emitir un secreto permanente reutilizable dentro del bundle Vue público.

### Q03 — Desactivar/renombrar dispositivo

**`PATCH /api/v1/gyms/{gymId}/checkin-devices/{deviceId}`** — **Nuevo**.

- **Pantalla:** administración de kioscos.
- **Permisos propuestos:** admin/owner.
- **Request:** `{ "deviceName":"Entrada norte", "active":false }`.
- **Comportamiento:** validar que pertenece a `gymId`; al desactivar invalida credenciales y tokens activos para operaciones futuras; nunca borrar `checkin_events` históricos.

### Q04 — Emitir QR efímero para kiosco

**`POST /api/v1/kiosks/{deviceId}/qr-token`** — **Nuevo**.

- **Pantalla:** `StandaloneCheckInPage.vue` **solo cuando corre como kiosco emparejado**.
- **Autenticación:** credencial limitada **del dispositivo**, NO JWT de miembro ni endpoint anónimo abierto. Vincular credencial a `deviceId` y `gymId`.
- **Comportamiento:** verificar dispositivo activo, gym activo, módulo habilitado; crear token criptográfico impredecible, guardar únicamente **hash** en `checkin_qr_tokens` y respetar `gym_settings.checkin_qr_ttl_seconds` (hoy la DB permite 5–300 segundos).
- **Respuesta:** `{ "token":"OPACO_CORTA_DURACION", "expiresAt":"...", "ttlSeconds":10, "gymId":"..." }`. El token solo debe mostrarse como QR, no dejarse en logs o analytics.
- **Política QR:** un QR efímero puede ser usado por más de una persona distinta mientras esté vigente; anti-replay aplica por identidad y acceso/día, además de comprobación de expiración y controles antifraude. Si se decide un token de un solo uso **global**, deberá renovarse tras cada lectura y documentarse su impacto operativo.

### Q05 — Registrar acceso de miembro leyendo QR de kiosco

**`POST /api/v1/gyms/{gymId}/checkins/validate`** — **Nuevo**.

- **Pantalla:** `CheckInPage.vue` desde móvil autenticado.
- **Permisos:** JWT del miembro, módulo habilitado, perfil de miembro del gimnasio.
- **Request:** `{ "qrToken": "OPACO_CORTA_DURACION" }`.
- **Comportamiento transaccional:** hash y validación de token/dispositivo/gym/caducidad, obtener **memberId desde JWT**, evaluar `v_member_access` (pago vigente, activo, gimnasio correcto), calcular `visit_date` en timezone del gimnasio, evitar duplicado y registrar `checkin_events` autorizado/denegado. No aceptar `memberId` libre en este endpoint.
- **Respuesta:** `200` o `201` `{ "eventId":"...","allowed":true,"reason":"granted","visitDate":"2026-11-03" }`. En denegaciones de negocio se devuelve resultado explícito y auditable, con política uniforme de códigos HTTP.
- **Concurrencia:** índices SQL `ux_member_daily_entry`, etc., ya impiden accesos concedidos duplicados por fecha; capturar ese conflicto de forma controlada.

### Q06 — Registrar acceso por código desde kiosco

**`POST /api/v1/kiosks/{deviceId}/checkins`** — **Nuevo**.

- **Pantalla:** `StandaloneCheckInPage.vue` cuando se ingresa manualmente un código.
- **Autenticación:** dispositivo emparejado y activo; NO `permitAll` sin control.
- **Request:** `{ "memberCode": "M-00025" }`.
- **Comportamiento:** resolver miembro exclusivamente por `(gym_id del dispositivo, member_code)`, aplicar reglas de acceso vigentes, auditar incluso denegación sin exponer detalles personales innecesarios. Rate limit por dispositivo e intentos fallidos.
- **Respuesta:** `{ "allowed": false, "reason": "expired", "visitDate":"2026-11-03" }` o resultado concedido.

### Datos y límites actuales

- `checkin_devices`, `checkin_qr_tokens` y `checkin_events` **ya existen**, de modo que no requieren tablas duplicadas.
- El `POST /gyms/{gymId}/checkins` **manual administrativo existente** se conserva tal cual y no debe quedar disponible para un visitante anónimo.
- `MemberDetailModal.vue` genera actualmente un texto identificativo `GMS-MEMBER:{code}`; no confundir este código estático con un QR temporal seguro para autorizar accesos.
- El frontend tiene estados de cámara deliberadamente no implementados: la integración QR requiere cambios futuros de Vue, permisos de cámara y HTTPS.

---

## 7. Archivos e imágenes (P1)

### F01 — Subir archivos del gimnasio

**`POST /api/v1/gyms/{gymId}/media-assets/upload`** — **Nuevo**.

- **Objetivo:** permitir subir imágenes/fotos para landing y fichas de miembros sin obligar a pegar una URL HTTPS externa.
- **Pantallas:** `MediaUploader.vue`, `MediaLibraryPage.vue`, `MemberRegistrationForm.vue` (foto).
- **Permisos propuestos:** admin/owner para medios públicos; para fotografía de miembro, operador autorizado que administra miembros. Definir una política de medios **privados** para fotos de personas: no tratarlas automáticamente como imágenes públicas de landing.
- **Solicitud:** `multipart/form-data`, campo `file`, opcionales `name`, `usage` (`landing`/`member_photo`).
- **Comportamiento:** validar contenido real además de extensión/MIME, límites de tamaño y dimensiones, almacenamiento externo, calcular clave segura, insertar `media_assets` con `storage_key` y `external_url=null`, `uploaded_by=actor.id()`. Evitar path traversal, archivos ejecutables, SVG/script si no se sanitiza y uso de URL controlada por cliente como destino de carga.
- **Respuesta 201:** `{ "id":"...", "gymId":"...", "name":"hero.jpg", "mediaType":"image", "mimeType":"image/jpeg", "byteSize":123456, "url":"https://cdn.ejemplo.com/..." }`. `url` puede ser temporal o público según política de visibilidad; no exponer `storage_key` interno si es sensible.
- **Integración:** guardar el `id` en `gym_members.photo_asset_id` o usarlo en los bloques; el GET de miembros debe resolver `photoUrl` bajo permisos. Los medios publicados necesitan una URL accesible a visitantes **solo para los assets publicados**.
- **Condicionado por almacenamiento:** si se decide almacenar los binarios directamente detrás de Spring, hará falta **otro endpoint seguro de descarga**. Si se usa CDN/almacenamiento firmado no inventar un GET de streaming adicional.

---

## 8. Dashboard y reportes (P2)

### D01 — Resumen agregado por gimnasio

**`GET /api/v1/gyms/{gymId}/dashboard/summary?from=2026-11-01&to=2026-11-30`** — **Nuevo**.

- **Pantalla:** `DashboardPage.vue`.
- **Objetivo:** obtener cifras correctas, porque `GET /payments`, `/sales` y `/checkins` devuelven listados limitados (no garantizan totales históricos).
- **Permisos propuestos:** owner/admin para cifras económicas; trainer puede recibir **otro DTO de métricas operativas** si su política lo permite, nunca datos económicos por defecto.
- **Respuesta ejemplo:**

```json
{
  "gymId": "00000000-0000-0000-0000-000000000010",
  "from": "2026-11-01",
  "to": "2026-11-30",
  "timezone": "America/Mexico_City",
  "currency": "MXN",
  "members": { "active": 125 },
  "checkins": { "granted": 430, "denied": 12 },
  "sales": { "count": 75, "revenue": 12500.00 },
  "payments": { "count": 61, "collected": 28000.00 },
  "classes": { "scheduled": 55, "completed": 42 }
}
```

- **Comportamiento:** SQL agregado con filtros `(gym_id, fechas)`, reglas explícitas de fecha local por gimnasio, moneda única, paginación no aplicada a totales, distinguir ingresos por POS vs cobros de membresía y proteger tiempos de respuesta con índices.
- **No inventar:** `occupancy`/aforo actual debe omitirse o declararse `null` si no existe fuente confiable de salidas/capacidad. El frontend ya lo trata así.

---

## 9. Invitados: SOLO si la pantalla deja de ser informativa (Futuro)

`GuestAccessPage.vue` es principalmente informativa y el backend no tiene un flujo de pases. La tabla `guest_passes` existe, pero **no** es un rol global ni exige cuenta en `users`.

### G01 — Emitir pase

**`POST /api/v1/gyms/{gymId}/guest-passes`** — **Condicional**.

- **Pantalla:** futura gestión de invitados, operador del gimnasio.
- **Permisos:** owner/trainer autorizado; nunca creación libre anónima sin antifraude.
- **Request:** `{ "fullName":"Invitado", "phone":"+52...", "expiresAt":"..." }`.
- **Comportamiento:** normalizar teléfono, validar límites por gimnasio, emitir pase temporal `issued`, no activar membresía permanente, no crear usuario global, registrar auditoría.
- **Respuesta:** 201 con identificador y estado; no prometer reservas hasta definir política.

### G02 — Consultar pases

**`GET /api/v1/gyms/{gymId}/guest-passes?status=issued&limit=50`** — **Condicional**.

- **Permisos:** personal autorizado.
- **Comportamiento:** filtrar gym/status, paginar y ocultar teléfonos en vistas no autorizadas.

### G03 — Revocar pase

**`POST /api/v1/gyms/{gymId}/guest-passes/{id}/revoke`** — **Condicional**.

- **Permisos:** owner o personal con permiso explícito.
- **Comportamiento:** estado `revoked` sin borrar historial; no aceptar reservas o accesos posteriores.

**No se propone por ahora una API pública de auto-registro de invitados ni reservas gratuitas**: faltan reglas de consentimiento, caducidad, cupos, verificación telefónica y antifraude.

---

## 10. CORRECCIONES de backend EXISTENTE (no crear ruta duplicada)

| Prioridad | Código/endpoint actual | Corrección concreta y motivo | Pantalla afectada |
|---|---|---|---|
| P1 | `MemberRepository.DETAIL` | Incluir `m.user_id AS user_id` (hoy no viene en la consulta) y un DTO consistente para que `mapMember` pueda resolver al usuario autenticado; revisar `photoUrl`. | `CheckInPage.vue`, `useMemberStore.js`. |
| P1 | `StaffService.all()` | Hoy exige owner; definir si trainer puede consultar **solo datos limitados** de staff. No abrir escritura por resolver lectura. | `useUserStore.js`, `UsersPage.vue`. |
| P1 | `GymService.change()` | Hoy solo admin global puede modificar datos del gimnasio. Mantener ese alcance; **L04** sería excepción acotada para owner sobre el slug, no un PATCH global sin filtro. | `LandingBuilderPage.vue`. |
| P0 | `SettingsPatch.landingPublished` | `PATCH /settings` **ya permite cambiar el booleano**, pero para un flujo editorial consistente debe canalizarse por L02/L03 (snapshot, validación, auditoría, invalidación). Evitar **dos maneras contradictorias** de publicar. | Builder / backend. |
| P0 | `SecurityConfig` | Actualmente `permitAll` solo a `/health` y `/auth/login`. Abrir **únicamente `GET /api/v1/public/gyms/*/landing`**; el resto continúa autenticado. Añadir seguridad propia para kioscos en caso de habilitarlos. | Landing pública. |
| P0 | `GymRequest` / `GymService.add()` | Hoy `slug` es obligatorio y se valida `[a-z0-9-]{3,80}`. Para autogenerarlo desde nombre, admitir `slug=null` **solo en alta**, generar/verificar unicidad con DB y manejar 409/reintentos. | `GymsPage.vue`; enlace. |
| P1 | `ReservationService.book()` | Validar conflictos de horario según regla aprobada, además de cupo, duplicados y límite de clase normal/functional actual. Mantener lock transaccional. | `CalendarPage.vue`. |
| P1 | `GymClassService` / listados | El DELETE cancela clases pero pueden seguir listándose sin filtrar `status`; añadir filtros explícitos, no devolver clases canceladas como reservables. | Calendario. |
| P1 | `ClassTemplateService` / listados | Inactivar plantilla no debe aparentar eliminación definitiva si se lista por defecto; filtrar `isActive` u ofrecer filtro de histórico. | Plantillas. |
| P1 | `ProductService` / listados POS | Producto desactivado no debe poder venderse ni mostrarse como disponible; conservarlo en historial de venta. | `PosPage.vue`. |
| P1 | `RoutineService` | Revisar `requireModule(gymId,"routines",...)` frente a validación de rol simple actual, para respetar el módulo deshabilitado en backend. | Rutinas. |
| P1 | `GET /members` / `GET /payments` | Un `member` nunca debe tener acceso global a listados administrativos para mostrar datos propios. Usar M02 y C01; registrar el pago solo por personal autorizado. | Miembro. |
| P1 | `GymService.remove()` / FKs | Hoy usa `hardDelete`, pero hay FKs `ON DELETE RESTRICT` desde staff, members, landing y demás. Definir regla segura de rechazo 409 cuando tenga datos; **no** agregar CASCADE destructivo indiscriminado ni borrado lógico oculto. | `GymsPage.vue`. |
| P2 | `SettingsRepository.patch()` | Construye nombres SQL desde un mapa interno generado por Service; mantener lista blanca estricta y actualizar `updated_by` cuando exista. Evitar que un PATCH lateral modifique publicación fuera de L02/L03. | Configuración. |
| P2 | `ApiExceptionHandler` | Hoy `DomainException` es 400 y `DataIntegrityViolationException` es 409 genérico. Añadir códigos de negocio claros para `slug_taken`, `landing_not_ready`, `already_registered`, `duplicate_checkin`, etc., **sin exponer SQL**. | Todos. |
| P2 | API listados / agregaciones | Uniformar límites, paginación y timezone, separar listados limitados de métricas agregadas; evitar calcular totales de una página de 50/100 registros. | Dashboard. |
| P2 | `AuthService`, `/auth/me` | Conservar contrato existente `user + gyms[roles[]]`, sin suponer `user.role` global. Cambios de roles deben reflejarse en `/auth/me`. | Selector y navegación. |

**Nota sobre el frontend:** estas correcciones son del backend, pero Vue también tendrá que conectarse a las rutas nuevas. En particular, `HomePage.vue` aún usa el store de bloques del gimnasio activo autenticado y el router actual solo tiene `/` como landing, no `/g/:slug`.

---

## 11. Arquitectura obligatoria para implementarlos después

Seguir exactamente la guía actual:

```text
HTTP / Vue
  |
  v
modules/<dominio>/routes/*Controller.java
  |  -> DTOs, @Valid, RequestMapping, status HTTP
  v
modules/<dominio>/services/*Service.java
  |  -> auth, tenant, reglas de negocio, @Transactional
  v
modules/<dominio>/interfaces/I*Repository.java
  |
  v
modules/<dominio>/repositories/*Repository.java
  |  -> BaseCRUD/JPA o JdbcTemplate encapsulado
  v
PostgreSQL (schema_db; migraciones Flyway)
```

### 11.1 Carpetas/clases sugeridas (nombres orientativos)

| Dominio | Controladores nuevos | Services/interfaces/repositorios |
|---|---|---|
| Identidades | `modules/users/routes/UserController` | `UserService`, `IUserRepository`, `UserRepository`, `dto/UserCreateRequest`, `UserPatchRequest`, `UserView`; aprovechar `security/IIdentityRepository` donde corresponda sin convertirlo en repositorio de todos los CRUD. |
| Registro | `modules/members/routes/MemberRegistrationController` (o ampliar `MemberController`) | Caso de uso en `MemberRegistrationService`, coordinando interfaces de usuarios y miembros en **una transacción**. |
| Staff guiado | `modules/staff/routes/StaffRegistrationController` | `StaffRegistrationService`, sin permitir creación global libre a owners. |
| Perfil miembro | Ampliar `MemberController` con `/me` | `MemberService` + `IMemberRepository` por `(gymId, actorId)`. |
| Reservas | Ampliar `ReservationController` con `/me`; `GymClassController` disponibilidad | `ReservationService`, `GymClassService`, repositorios con conteos y aislamiento. |
| Landing | `modules/landing/routes/LandingPublicationController`, `PublicLandingController` | `LandingPublicationService`, `PublicLandingService`, `ILandingPublicationRepository`, `LandingPublicationRepository`, DTOs **públicos** específicos. Reutilizar `SettingsRepository`/`IGymRepository` mediante interfaces apropiadas. |
| Archivos | Ampliar `MediaAssetController` con `/upload` | `MediaStorageService` (abstracción de proveedor), `MediaAssetService`, `IMediaAssetRepository`. |
| Check-in | `CheckinDeviceController`, `KioskController`, ampliar `CheckinController` | `CheckinDeviceService`, `KioskTokenService`, `CheckinValidationService` y repositorios tipados. |
| Dashboard | `DashboardController` | `DashboardService`, `IDashboardRepository`, `DashboardRepository` con SQL agregados. |
| Invitados | `GuestPassController` | `GuestPassService`, `IGuestPassRepository`, `GuestPassRepository`. |

En módulos donde el proyecto actual registra Services desde `shared/factories/ServiceFactory.java`, hacer el registro de dependencias ahí o adoptar una estrategia coherente sin duplicar beans.

### 11.2 Reglas que no deben romperse

1. Controller **no** accede a SQL ni implementa reglas de negocio.
2. Service **no** accede a `JdbcTemplate`/`EntityManager`; usa `I*Repository`.
3. Todo registro tenant se lee/escribe con `gymId` controlado por servidor y actor de JWT.
4. `@Transactional` en publish, register y check-in, con rollback en fallos.
5. DTOs de entrada con lista blanca, `@Valid`, sin mass assignment desde JSON.
6. `BaseCRUD`, `BaseService`, `AuditService` y factoría se reutilizan cuando aplican, **sin escribir campos `created_by/deleted_at` que no existen** en las tablas correspondientes.
7. Migraciones **aditivas**, nunca editar V1/V2 ya aplicadas ni ejecutar cambios directos sin Flyway. Utilizar el siguiente número libre de migración, tras revisar `flyway_schema_history` del entorno real.
8. No crear nuevas tablas para `gym.slug`, `landing_published`, `media_assets`, dispositivos QR ni pases de invitado porque **ya existen**.
9. Los roles efectivos salen de `platform_admins`, `gym_staff_roles` y `gym_members`, no de una columna inexistente en `users`.
10. Privacidad y multi-gym se prueban sobre DB real, no únicamente con mocks.

---

## 12. Cambios propuestos en la base de datos y de configuración

| Necesidad | ¿Tabla/columna nueva? | Acción recomendada |
|---|---|---|
| Slug personalizable | **No** | Usar `gym.slug CITEXT UNIQUE`. Normalización y control en Service; opción futura de tabla de alias si hay redirecciones. |
| Publicar/despublicar | **No**, para el booleano | `gym_settings.landing_published` ya existe. |
| Borrador vs versión pública | **Sí, recomendado** | `landing_publications` con snapshot JSONB/version/fecha/actor, mediante Flyway aditiva. |
| Listar medios y foto | **No**, para referencia | `media_assets` y `gym_members.photo_asset_id` ya existen; hace falta almacenamiento externo y DTO de URL. |
| Alta de cuentas | **No**, para tabla | `users` ya existe; Service, autorización, hash y repositorio de escritura. |
| Alta miembro/staff | **No**, para tablas | `gym_members`, `gym_staff_roles` existentes. |
| Historial propio y reservas | **No** | Consultas por `userId` actor y `gymId` usando `checkin_events` y `class_reservations`. |
| Kioscos, tokens y eventos | **No**, para modelo inicial | `checkin_devices`, `checkin_qr_tokens`, `checkin_events` existentes; verificar si hacen falta campos adicionales de pairing/rotación de credenciales según diseño final. |
| Dashboard | **No obligatorio** | Consultas agregadas en repositorio; optimización con índices según EXPLAIN ANALYZE. |
| Invitados | **No**, para tabla | `guest_passes` existente; falta Service y política de negocio. |

**Configuración:** agregar una propiedad validada para `PUBLIC_SITE_BASE_URL`; revisar CORS si el dominio de la landing es distinto del panel. Para carga de medios configurar proveedor, límites, acceso CDN y secretos por entorno, nunca en fuente. Para kioscos configurar autenticación separada, cookies/credenciales seguras y protección frente a intentos abusivos.

---

## 13. Contrato de errores, permisos y seguridad

### 13.1 Convención propuesta para nuevos endpoints

```json
{
  "error": "slug_taken",
  "message": "El enlace elegido ya pertenece a otro gimnasio",
  "details": { "field": "slug" }
}
```

| HTTP | Cuándo aplicarlo |
|---|---|
| `200` | Consulta/cambio exitoso; validación de acceso con respuesta explícita. |
| `201` | Cuenta, miembro, archivo, dispositivo o pase creado. |
| `204` | DELETE sin body de rutas existentes. |
| `400` | Formato inválido, UUID mal formado, campos faltantes. |
| `401` | Falta autenticación o token inválido en endpoint privado. |
| `403` | Actor autenticado sin permiso o módulo deshabilitado según política. |
| `404` | Recurso no encontrado; landing pública no existente/no publicada/suspendida. |
| `409` | Correo/slug/código duplicado, conflicto de estado o concurrencia. |
| `422` | Reglas de negocio semánticas (página no lista, acceso vencido si se usa HTTP de rechazo); requiere ampliar excepción actual. |
| `429` | Exceso de solicitudes, sobre todo búsquedas de usuarios y kioscos. |

El handler actual **no distingue todos estos casos**: se propone unificar respuestas desde `ApiExceptionHandler` sin filtrar consultas SQL, hashes o información personal.

### 13.2 Matriz de autorización sugerida (PENDIENTE DE APROBACIÓN)

| Operación | Admin global | Owner del gym | Trainer del gym | Member del gym | Anónimo |
|---|---|---|---|---|---|
| Crear/editar cuentas globales | Sí | No (usar flujo acotado) | No | No | No |
| Alta miembro en gym | Sí | Sí | Por definir | No | No |
| Vincular staff existente | Sí | Sí, solo trainer según política hoy | No | No | No |
| Publicar/despublicar/slug | Sí | Sí | No por defecto | No | No |
| Vista previa/editar bloques | Sí | Sí | Según permiso existente o aprobado | No | No |
| Consultar landing publicada | Sí | Sí | Sí | Sí | **Sí** |
| Consultar propio expediente/reservas/accesos | Solo si también es miembro | Si también es miembro | Si también es miembro | Sí | No |
| Administrar kioscos | Sí | Sí | No por defecto | No | No |
| Leer resumen financiero | Sí | Sí | No por defecto | No | No |

Una persona puede reunir roles simultáneos. **Nunca** interpretar `owner` como rol global ni permitir acceso a otro `gymId` porque exista una cuenta global.

### 13.3 Endpoint público: excepción mínima

La clase `security/SecurityConfig.java` actual tiene `permitAll` solamente para `/api/v1/health` y `/api/v1/auth/login`. Al implementar L05, permitir **solo `GET`** de la landing pública y mantener POST/PATCH/DELETE privados. Verificar la configuración CORS y limitar el contenido por DTO; **permitAll no suple filtros de publicación**.

Los endpoints Q04/Q06 se autentican con credenciales **de dispositivo** gestionadas separadamente. Que el frontend del kiosco sea visualmente una página pública **no autoriza** llamadas de registro de entrada sin controles.

---

## 14. Matriz de integración por pantalla (qué deberá consumir Vue cuando se conecte)

| Pantalla/componente Vue | Llamadas necesarias | Qué debe mostrar o hacer |
|---|---|---|
| `LandingBuilderPage.vue` | CRUD existente `landing-blocks`; GET L01, POST L02/L03, PATCH L04 | Guardar borradores, publicar, despublicar, mostrar/copiar URL, indicar cambios pendientes. |
| `HomePage.vue` + router `/g/:slug` | GET L05 **sin bearer** | Leer el slug de la URL, cargar tema y bloques públicos, 404 si no está publicada. |
| `GymsPage.vue` | `POST /gyms` existente **ajustado** | Generar slug desde nombre si omitido; mostrar error 409 por colisión. |
| `UsersPage.vue` admin | U01–U05; `POST /staff` existente para asignación | Alta global y edición de perfil/estado; asignación de roles por gym separada. |
| `UsersPage.vue` owner | U06 si aprobado; `POST /staff` existente; S01 condicional | Alta trainer o vínculo autorizado; no acceso a cuentas de otros gyms. |
| `MemberRegistrationForm.vue` | M01; U06 y `POST /members` existentes si cuenta ya creada; F01 para foto | Quitar requisito de escribir UUID manual cuando se autorice alta por API. |
| `MembersPage.vue`, `MemberDetailModal.vue` | CRUD existente, `photoUrl` resuelta | Gestión de expedientes, inactivación y foto. |
| `CheckInPage.vue` | M02, C01, Q05 | Comprobar estado propio, escanear QR y mostrar historial. |
| `StandaloneCheckInPage.vue` | Q04, Q06 tras emparejamiento | Mostrar QR rotativo, entrada por código, mensajes reales sin datos personales. |
| `CalendarPage.vue`, `DayScheduleModal.vue` | GET clases existente con filtros, R01, R02, POST/DELETE reservas existentes | Cupos, reserva propia, cancelación y conflictos de horario. |
| `MediaUploader.vue` | F01; CRUD media existente | Subir y seleccionar archivos persistidos. |
| `DashboardPage.vue` | D01 | Indicadores correctos por fecha, sin sumar únicamente los últimos registros. |
| `GuestAccessPage.vue` | Ninguno indispensable ahora; G01–G03 solo al habilitar pases | Mantener informativa hasta aprobar flujo de invitados. |

**Tarea futura de Vue:** `src/api/endpoints.js` y `src/api/service.js` deben añadir estas rutas cuando existan; el router actual no define `/g/:slug`; `useLandingStore.js` hoy consulta solo `/landing-blocks` con `gymId` activo y no debe ser el store que alimenta una vista anónima. La previsualización del builder puede seguir usando el borrador local autenticado.

---

## 15. Pruebas de aceptación obligatorias antes de declarar terminado

### Landing, publicación y slug

- [ ] Crear gym "Iron Fitness" genera o admite `iron-fitness` y la UI construye `/g/iron-fitness` con dominio de configuración.
- [ ] Dos gimnasios no pueden usar slug igual ignorando mayúsculas; el conflicto retorna 409 sin cambiar el existente.
- [ ] Gym sin publicar devuelve 404 desde GET público, incluso sin JWT; el editor autenticado sí ve borrador.
- [ ] Owner A no puede publicar, cambiar slug ni leer borrador administrativo del Gym B.
- [ ] Publicar devuelve enlace y snapshot; GET público ofrece **solo** bloques habilitados, ordenados y campos aprobados.
- [ ] Cambiar un bloque de borrador no modifica la vista pública hasta republicar (si se usa snapshot recomendado).
- [ ] Republicar actualiza versión y contenido sin perder otros bloques.
- [ ] Despublicar hace que API pública devuelva 404 **también tras limpiar caché** y conserva borrador y medios.
- [ ] Gym suspendido/archivado nunca muestra landing aunque `landing_published=true`.
- [ ] No se devuelven hashes, correos, pagos, nombres de miembros ni otros datos internos en JSON público.
- [ ] URL vieja después de cambiar slug tiene el comportamiento documentado: 404, o redirección si se implementan alias.
- [ ] Ruta `/g/:slug` funciona al pegarla en navegador, refrescarla y compartirla en una ventana incógnita.

### Cuentas y multi-gimnasio

- [ ] Crear cuenta produce hash BCrypt y no contiene `passwordHash` en respuesta/logs.
- [ ] Mismo correo (CITEXT) retorna 409; cuentas globales no se duplican por gimnasio.
- [ ] Registrar miembro crea cuenta+expediente atómicamente y no registra cobro implícito.
- [ ] Error en expediente provoca rollback de cuenta; no queda persona huérfana.
- [ ] Owner A no puede alterar una cuenta de B por usar un UUID conocido.
- [ ] Deshabilitar usuario impide autenticación y acceso con JWT en solicitudes nuevas.
- [ ] `/auth/me` mantiene `gyms[]` con `roles[]` por gimnasio; ningún `user.role` global.
- [ ] `GET /members/me` jamás devuelve expediente de otra persona.

### Calendario y check-in

- [ ] Miembro ve solo sus reservas; no obtiene listado de asistentes de toda la clase.
- [ ] Cupos se validan bajo concurrencia; una pantalla que indique 1 cupo no permite 2 reservas simultáneas.
- [ ] QR vencido, de otro gimnasio o de kiosco desactivado es rechazado y auditado.
- [ ] Check-in calcula fecha en timezone de gym y evita dos accesos concedidos del mismo miembro ese día.
- [ ] Un kiosco no emparejado no puede generar token ni registrar entrada por código.
- [ ] Logs no guardan token de QR sin hash ni secretos de dispositivo.

### Medios, datos y operación

- [ ] Subida real valida tamaño y MIME; archivo malicioso se rechaza; storage_key se guarda de forma segura.
- [ ] Foto privada de miembro no es visible desde landing pública por conocer el asset ID.
- [ ] Dashboard agrega todo el rango solicitado y diferencia ingresos de POS y pagos de membresías.
- [ ] `mvn clean test` pasa; migración nueva aplica sobre base **nueva y existente**, sin editar checksums de V1/V2.
- [ ] Pruebas HTTP y DB real verifican tenant, roles, estados, índices y rollback de casos críticos.

---

## 16. Orden recomendado de desarrollo, sin duplicar trabajo

### Fase A — Página pública compartible (**primero para el requerimiento nuevo**)

1. Acordar contrato de slug y nombre, y decidir snapshot editorial.
2. Agregar migración aditiva `landing_publications` **si se aprueba**.
3. Implementar L01–L05 en capas; restringir `GET` público en seguridad.
4. Ajustar `POST /gyms` existente para autogenerar slug (si se aprueba).
5. Probar anónimo, despublicación, edición de borrador, aislamiento y 404.
6. **Después**, integrar Vue `/g/:slug`, estado de publicación y botón compartir.

### Fase B — Altas reales de usuarios/miembros (tras aprobar cambio de decisión)

1. Actualizar documentación maestra que hoy impone provisión SQL.
2. Implementar U01–U06 con permisos acotados y protección de identidad.
3. Implementar M01 y M02; S01 solo si se desea alta de trainer en un paso.
4. Arreglar `MemberRepository.DETAIL` y probar multi-gimnasio.

### Fase C — Calendario, datos propios y archivos

1. R01, R02, C01; ampliar GET de clases para rangos/estados.
2. F01 y resolución de fotos/medios con privacidad.
3. Corregir cancelación, listados y reglas de reserva.

### Fase D — QR seguro, si el módulo estará activo

1. Diseñar y aprobar el emparejamiento seguro de kiosco.
2. Q01–Q06 y pruebas de tokens, rate limits y acceso.
3. Integrar escáner/QR real en Vue, sin contraseñas embebidas.

### Fase E — Complementos

1. D01 y métricas por periodo.
2. G01–G03 **solo** si el acceso de invitados se convierte en función real.

**Regla para todas las fases:** actualizar en el mismo cambio **código + tests + docs + contrato + migración (si aplica)**, siguiendo `README_MASTER_BACKEND.md`, `docs/VERIFICACION.md` y `docs/REGISTRO_CAMBIOS.md`.

---

## 17. Resumen final de decisiones que debes aprobar antes de programar

1. **¿Alta de usuarios por API?** Las guías actuales ordenan hacerlo solo por SQL; este documento propone reemplazar esa limitación para satisfacer `UsersPage.vue` y el alta de miembros nuevos.
2. **¿Solo admin crea usuarios globales?** Recomendado. Owner usa alta de miembro o trainer acotada a su gym.
3. **¿Publicación con snapshot?** Recomendado para que guardar borrador no cambie la página pública automáticamente.
4. **¿Owner cambia slug?** Recomendado, exclusivamente mediante L04 y no mediante el PATCH global del gimnasio.
5. **¿Qué pasa con enlace antiguo tras cambiar slug?** Primera entrega: 404 advertido; alternativa futura: tabla de alias + redirección.
6. **¿Quién puede editar/publicar landing?** Admin global y owner propuestos; mantener criterio de permisos por gimnasio, no un rol global inventado.
7. **¿QR y kioscos entran en primera entrega?** No confundir pantalla visible con funcionalidad implementada; exige pairing seguro y controles de acceso.
8. **¿Invitados siguen informativos?** Si sí, G01–G03 quedan fuera del MVP.

### Archivos revisados que fundamentan esta especificación

- Backend: `README_MASTER_BACKEND.md`, `docs/{ARQUITECTURA_Y_PATRONES,CONTRATOS_FRONTEND,DECISIONES_PENDIENTES,SEGURIDAD_Y_BD,VERIFICACION}.md`.
- Backend: `security/{SecurityConfig,AuthorizationService,IIdentityRepository}.java`, `shared/factories/ServiceFactory.java`, `shared/api/ApiExceptionHandler.java`.
- Backend: `modules/{gyms,settings,landing,media,members,staff,calendar,checkin,plans,payments,pos,routines}/...`.
- DB: `src/main/resources/db/migration/V1__base_schema.sql` y `V2__allow_functional_calendar_category.sql`.
- Frontend: `src/router/index.js`, `src/api/{endpoints,service,mappers}.js`, `src/pages/dashboard/{LandingBuilderPage,UsersPage,MembersPage,CheckInPage,CalendarPage,DashboardPage}.vue`, `src/pages/public/{HomePage,StandaloneCheckInPage}.vue`, `src/components/{dashboard/MediaUploader,members/MemberRegistrationForm}.vue`, stores `useLandingStore`, `useMemberStore`, `useUserStore`, `useCheckinStore`.

**Estado:** especificación propuesta elaborada por inspección estática de ambos ZIP. No se ejecutaron pruebas HTTP ni cambios sobre el backend, frontend o PostgreSQL.
