# GMS — Especificación técnica de evolución del backend

**Versión:** 1.0 · **Fecha:** 05/11/2026 · **Estado:** propuesta de implementación, aún no aplicada.

**Base contrastada:** `DIAGNOSTICO_INTEGRACION_TOTAL_FRONTEND_GMS.md` y código del último ZIP disponible, `gym_back_refactor_jpa_guia_pagina_publica_actualizada.zip` (revisión estática). **No se ejecutaron Maven, PostgreSQL ni pruebas de extremo a extremo para elaborar esta especificación.**

## 0. Alcance aprobado y decisiones cerradas

| Tema | Decisión obligatoria | Qué se implementa |
|---|---|---|
| QR: miembro escanea gimnasio | **Sí** | Kiosco autorizado emite QR efímero; miembro autenticado lo escanea; backend valida y registra. |
| QR: recepción escanea miembro | **Sí** | Miembro autenticado muestra credencial QR temporal; dispositivo/recepción autorizada la verifica y registra. |
| Kiosco | **Solo dispositivo autorizado por gimnasio** | La URL `/checkin?gym=<slug>` es accesible, pero escanear, emitir tokens y registrar entradas exige sesión de dispositivo. |
| Clase muestra | **Solo informativa** | No crear API de invitaciones, pases ni reservas de invitado. Conservar pantalla y mensaje informativo. |
| Ocupación | **Sin aforo en tiempo real** | Métricas reales de accesos concedidos/denegados por período y hora; jamás afirmar cuántas personas están dentro. |
| Notificaciones | **Locales al frontend** | No crear tablas, cron, servicios ni endpoints de notificaciones persistentes. |
| Landing | **Publicación automática por gimnasio activo** | No crear `publish/unpublish`; respetar `/g/:slug` y los endpoints de slug/tema/media actuales. |
| Datos visibles | **Sin identificadores técnicos** | ID y tokens solo en transporte/estado interno; no en textos ni respuestas públicas sensibles. |
| Persistencia | **JPA + interfaces + BaseCRUD/BaseService** | Nada de `JdbcTemplate`, SQL construido dinámicamente o SQL en Controller/Service. Flyway es la excepción necesaria para el esquema. |

**Fuera de alcance:** reescritura del frontend; publicar/despublicar landing; WhatsApp/correo; clase muestra real; check-out/aforo en vivo; cambiar el flujo vigente de propietarios/entrenadores; crear notificaciones persistentes; habilitar códigos estáticos como credencial anónima de acceso.

## 1. Resultado de la comparación con el backend actual

### 1.1 Reutilizable y confirmado en código

- `modules/checkin/routes/CheckinController.java`: existen `GET /api/v1/gyms/{gymId}/checkins`, `GET .../checkins/me`, `POST .../checkins`.
- `modules/checkin/services/CheckinService.java`: registro manual privado; valida módulo/rol; usa `ICheckinRepository` y la regla de acceso. **No tiene lógica QR**.
- `modules/checkin/repositories/CheckinRepository.java`: usa `EntityManager`/JPQL, evalúa `v_member_access`, consulta duplicados y persiste `CheckinEntry`.
- `shared/persistence/model/CheckinEntry.java`: mapea `checkin_events`, tiene borrado lógico y `method`, `allowed`, `reason_code`, `device_id`, etc.
- **Flyway V1** ya crea `checkin_devices`, `checkin_qr_tokens`, `checkin_events`. `checkin_qr_tokens` guarda **hash** de token e índice de expiración, no el token en claro. `checkin_events.method` permite `kiosk_code`, `kiosk_qr`, `member_qr`, `manual`, `guest`.
- **Flyway V4** incorpora auditoría/borrado lógico, define `v_member_access` y el índice único `ux_member_daily_entry`: **un único acceso concedido por miembro, gimnasio y fecha local**.
- `SecurityConfig.java` protege por JWT de usuario, salvo rutas públicas existentes. **No incluye autenticación de dispositivo/kiosco.**
- `AuthorizationService.java` distingue administrador global, owner/trainer/member por gimnasio, gimnasio activo y módulos habilitados.
- `AuthController.java` **sí** contiene `GET /api/v1/auth/me`, que devuelve `AuthSession(user, gyms)`.
- `DashboardRepository.java` y `GET /dashboard/summary` ya agregan accesos concedidos/denegados; el dashboard no necesita calcular totales sobre sus primeros 100 registros.
- `LandingPublicationController` y `PublicLandingController` implementan slug, link, logo y lectura pública. Se conservan.

### 1.2 Brechas reales identificadas

1. No hay modelos JPA, repositorios ni servicios Java operativos para `checkin_devices` y `checkin_qr_tokens`.
2. El registro actual siempre escribe `method="manual"`; no hay entradas QR correctamente diferenciadas.
3. No hay validación de QR del gimnasio ni credencial QR temporal del miembro.
4. No existe identidad de kiosco ni activación/revocación de dispositivo a nivel de API/autenticación.
5. `alreadyAllowed()` seguido de `record()` no resuelve por sí solo reintentos/concurrencia: el índice de base de datos es la salvaguarda final, pero falta una respuesta estable ante colisiones.
6. No hay endpoint de métricas de accesos para la pantalla específica con filtros y paginación explícitos; `GET /dashboard/summary` agrega indicadores generales y puede reutilizarse.
7. `v_member_access` puede devolver `allowed` cuando `last_payment_date` existe pero `next_due_date` es `NULL`; **se debe confirmar la regla de vigencia y no conceder por ausencia de datos**.
8. El frontend genera un QR `GMS-MEMBER:<member.code>`; eso identifica visualmente al miembro, pero **no demuestra autenticidad** ni constituye una credencial segura para registrar accesos.

**Principio de compatibilidad:** NO sustituir los tres endpoints de Check-in actuales; añadir rutas nuevas y clases especializadas. Mantener el formato del POST manual (`eventId`, `allowed`, `reason`, `visitDate`) y de las listas actuales. Los contratos nuevos podrán usar DTO tipados en `camelCase`.

## 2. Arquitectura obligatoria

```text
HTTP (usuario JWT o dispositivo autorizado)
    │
    ▼
Controller + @Valid DTO ──> SecurityFilterChain / DeviceAuthenticationFilter
    │
    ▼
CheckinService / KioskDeviceService / CheckinQrService
    │  ├─ AuthorizationService (usuarios, gimnasio, módulo)
    │  ├─ DeviceAuthorizationService (kiosco, gimnasio, revocación)
    │  └─ MemberAccessPolicy (misma regla para manual y ambos QR)
    ▼
I*Repository (contratos)
    │
    ▼
*Repository con JPA/JPQL/Criteria
    │  └─ BaseCRUD / BaseService para CRUD habitual
    ▼
PostgreSQL + Flyway V5 + restricciones únicas
```

**Distribución de responsabilidad:** Controller recibe/verifica formato y delega; Service define autorización, transacción y negocio; Repository consulta y persiste; `BaseCRUD` solo CRUD por tenant, filtro de no eliminados; `BaseService` solo utilidades CRUD compartidas. `CheckinService` ya es un servicio especializado y puede seguir siéndolo sin forzar herencia artificial. Los tokens/credenciales se encapsulan en componentes de seguridad, no en JPA ni en controladores.

**Transacciones:** cada intento de ingreso y su evento se gestionan por una transacción; si se confirma un acceso, el índice único en BD impide duplicado incluso con solicitudes paralelas. Los reintentos con la misma clave devuelven el mismo resultado; los denegados se registran sin insertar FK inválidas. En caso de conflicto único, manejar la transacción fallida desde un límite transaccional externo, no intentar persistir un nuevo evento dentro de la misma transacción abortada.

## 3. Identidad y seguridad del kiosco

### 3.1 Modelo de confianza

- `/checkin?gym=<slug>` es la **pantalla pública**; el slug identifica el gimnasio **para presentación**, no para autorizar operaciones.
- Solo un dispositivo **habilitado, activado, no borrado y ligado a un gimnasio activo** puede emitir QR o validar credenciales.
- Alta/revocación de dispositivos: **owner de ese gimnasio o administrador global**. Un trainer no administra credenciales de dispositivos.
- Un navegador común sin sesión de dispositivo solo puede ver nombre/estado público mínimo del gimnasio; no recibe lista de miembros, accesos, pagos, tokens QR ni estadísticas privadas.
- El kiosco no puede elegir libremente `gymId` al registrar acceso: el backend lo toma de la identidad de dispositivo y contrasta el slug cuando exista.
- Nunca enviar al navegador contraseñas de usuario, secretos duraderos del dispositivo o claves de firma; no guardar secretos duraderos en `localStorage`.

### 3.2 Provisión de dispositivos (propuesta cerrada para esta entrega)

1. Owner/admin crea `checkin_device` para su gimnasio; servidor genera código de activación aleatorio de alta entropía, de **un solo uso**, que expira en **10 minutos**. Solo se muestra una vez y se persiste su hash.
2. El operador abre `/checkin?gym=<slug>` en el dispositivo elegido e introduce/escanea dicho código; el backend valida gimnasio, expiración e intentos y marca el dispositivo como activado.
3. El backend entrega sesión de kiosco mediante **cookie `HttpOnly`, `Secure`, `SameSite=Strict`** si el frontend y API comparten sitio. El navegador usa `credentials: include` y el servidor habilita CORS con credenciales **solo para el origen frontend configurado**, nunca con comodín. La cookie identifica una sesión con hash en BD, vinculada al dispositivo. **Si frontend y API son cross-site**, se requiere diseño y prueba específica de `SameSite=None; Secure`, CORS con credenciales y protección CSRF; no habilitarlo por defecto.
4. Las operaciones con cookie deben validar **CSRF por header + origen confiable**. La configuración global actual deshabilita CSRF y necesitará un tratamiento explícito para las rutas de kiosco. El JWT del usuario sigue usando Bearer sin cambio.
5. El backend revisa `is_active`, `deleted_at`, gimnasio y vigencia de sesión **en cada operación**. `GET /kiosk/session` puede devolver un token CSRF de sesión (no un token de autenticación) para que el cliente lo envíe en `X-CSRF-Token`. La revocación invalida todas las sesiones del dispositivo.
6. Una sesión de kiosco dura **hasta 12 horas** (configurable); para volver a activar tras expiración se requiere un nuevo código emitido por owner/admin. No almacenar un secreto perpetuo en el frontend.

**Nota de seguridad:** la posesión física de un kiosco desbloqueado permite enviar solicitudes durante la sesión. Complementar con modo kiosco del dispositivo, bloqueo de pantalla y controles de red. La cookie protege secretos frente al JavaScript, pero no sustituye el control físico.

### 3.3 Seguridad de QR

- **QR del gimnasio:** valor aleatorio de al menos 256 bits, generado y entregado por servidor; se guarda solo su hash en `checkin_qr_tokens`. Vigencia inicial **10 s** (según comentario del esquema V1), configuración explícita y sin confianza en reloj del navegador. No se consume con la primera lectura: varios miembros pueden escanear el mismo QR durante su vigencia.
- **QR del miembro:** credencial **firmada, efímera** (propuesta: 30 s) emitida solo a miembro autenticado; contiene propósito `member-checkin`, gimnasio, referencia interna al miembro, expiración, identificador aleatorio `jti`, emisor y audiencia. Se valida firma, propósito, gimnasio y vigencia; además se revalida el estado actual del miembro en BD. Usar clave/uso criptográfico separado del access JWT habitual.
- **No aceptar** `GMS-MEMBER:<code>` como credencial autenticadora. Puede seguir existiendo como dato visual del expediente, pero el nuevo flujo debe utilizar QR temporal del servidor.
- Ambos QR usan HTTPS; respuestas con valores QR/token llevan `Cache-Control: no-store`; no registrar tokens en logs, analítica, URLs o herramientas de observabilidad.
- Los códigos de miembro, correos y teléfonos **no** se usan como un secreto que autorice entrada. El botón «Validar código» del frontend requiere un contrato adicional de credencial segura si se mantiene como alternativa; no reutilizar un código adivinable ni simular éxito.

### 3.4 Límites de abuso

- Endpoint de activación: por dispositivo e IP, máximo configurable (propuesta 5 intentos fallidos/15 min); no enumerar dispositivos mediante errores.
- Emisión QR: únicamente sesión de dispositivo válida; tasa y concurrentes limitados; no generar cientos de registros por segundo.
- Validación: límites por usuario/dispositivo/IP, controles de intentos, tiempos constantes para comparación de secretos y rechazo seguro ante código malformado.
- Para múltiples instancias, el control de tasa debe ser compartido (gateway/almacenamiento central), no solo un mapa en memoria JVM. Respuesta uniforme `429` con `Retry-After` cuando corresponda.
- Asegurar logs estructurados sin credenciales, PII ni tokens; correlación por `requestId` interno.

## 4. Contratos HTTP nuevos propuestos (NO existen todavía)

**Prefijo existente:** `/api/v1`. Todos los endpoints nuevos deben aparecer en OpenAPI y colección de pruebas. Los UUID viajan como identificadores internos, **nunca se imprimen en interfaz**.

### 4.1 Gestión de dispositivos — owner/admin

| Método y ruta | Permiso | Resultado |
|---|---|---|
| `GET /gyms/{gymId}/checkin-devices` | owner/admin | Lista de dispositivos con nombre, activo, último acceso y activación; sin hashes/secretos. |
| `POST /gyms/{gymId}/checkin-devices` | owner/admin | `201` dispositivo + código de activación **solo una vez**. |
| `POST /gyms/{gymId}/checkin-devices/{deviceId}/reissue-activation` | owner/admin | Nuevo código temporal; invalida anterior. |
| `PATCH /gyms/{gymId}/checkin-devices/{deviceId}` | owner/admin | Activar/desactivar (no reactivar sesión anterior). |
| `DELETE /gyms/{gymId}/checkin-devices/{deviceId}` | owner/admin | `204`; borrado lógico, revocación de sesiones y tokens. |

**Ejemplo de alta**:

```http
POST /api/v1/gyms/{gymId}/checkin-devices
Authorization: Bearer <accessToken>
Content-Type: application/json

{"deviceName":"Recepción principal"}
```

**Respuesta `201`** (código solo en esta respuesta, nunca en GET):

```json
{
  "deviceId":"<uuid-interno>",
  "deviceName":"Recepción principal",
  "active":true,
  "activationCode":"<valor-opaco-de-un-solo-uso>",
  "activationExpiresAt":"2026-11-05T12:00:00-06:00"
}
```

### 4.2 Pantalla pública y activación del dispositivo

| Método y ruta | Seguridad | Respuesta |
|---|---|---|
| `GET /public/gyms/{slug}/checkin-config` | Anónimo; solo gimnasio activo | Nombre y slug visuales; `checkinAvailable`; sin información privada. |
| `POST /kiosk/activate` | Código de activación + `slug`; rate-limit | `204` + cookie segura de sesión; código inutilizable después. |
| `GET /kiosk/session` | Cookie kiosco válida | Nombre visible del gimnasio, nombre del dispositivo, expiración y token CSRF de sesión para enviar en header (no es credencial de autenticación). |
| `POST /kiosk/logout` | Cookie + CSRF | `204` y revocación de sesión/cookie. |

**Ejemplo activación**:

```json
{"slug":"centro-neuro","activationCode":"<valor-temporal>"}
```

No aceptar un `gymId` enviado por cliente para reemplazar el gimnasio al que fue provisionado el dispositivo. `POST /kiosk/activate` se permite sin Bearer de usuario, pero **solo** tras comprobar activación válida de un dispositivo registrado.

### 4.3 Flujo A — miembro escanea QR del gimnasio

| Paso | Operación | Responsable |
|---|---|---|
| 1 | `POST /kiosk/qr/issue` | Dispositivo autorizado (cookie+CSRF). |
| 2 | Kiosco renderiza QR desde `qrValue`; refresca antes de vencimiento | Frontend. |
| 3 | `POST /gyms/{gymId}/checkins/scan-gym-qr` | Miembro autenticado (`Bearer`) y con rol member del mismo gym. |
| 4 | Servidor valida token, gimnasio, módulo, miembro, vigencia y duplicidad; escribe evento | `CheckinService`. |

**Emisión de QR (respuesta `200`)**:

```json
{
  "qrValue":"<token-opaco>",
  "expiresAt":"2026-11-05T11:38:10-06:00",
  "serverTime":"2026-11-05T11:38:00-06:00",
  "refreshAfterSeconds":8
}
```

**Escaneo por miembro**:

```http
POST /api/v1/gyms/{gymId}/checkins/scan-gym-qr
Authorization: Bearer <accessToken>
Idempotency-Key: <uuid-aleatorio-por-intento>
Content-Type: application/json

{"kioskToken":"<token-opaco>"}
```

**Respuesta de negocio `200` (concedido o denegado)**:

```json
{
  "allowed":true,
  "reasonCode":"granted",
  "message":"Acceso registrado correctamente.",
  "occurredAt":"2026-11-05T11:38:05-06:00",
  "visitDate":"2026-11-05"
}
```

Las denegaciones de negocio válidas (`expired`, `no_payment`, `duplicate`, etc.) también pueden responder `200` con `allowed:false` y texto seguro. Errores técnicos/autenticación usan su HTTP correspondiente.

### 4.4 Flujo B — recepción escanea QR del miembro

| Paso | Operación | Responsable |
|---|---|---|
| 1 | `POST /gyms/{gymId}/checkins/me/qr` | Miembro autenticado (Bearer). |
| 2 | Miembro muestra QR temporal en su sesión | Frontend privado. |
| 3 | `POST /kiosk/checkins/scan-member-qr` | Dispositivo autorizado (cookie + CSRF). |
| 4 | Servidor valida firma, expiración, gym, vínculo miembro y regla de acceso; registra evento | `CheckinService`. |

**Respuesta del QR del miembro (`200`)**:

```json
{
  "qrValue":"<credencial-firmada-efímera>",
  "expiresAt":"2026-11-05T11:38:30-06:00",
  "serverTime":"2026-11-05T11:38:00-06:00"
}
```

**Escaneo de recepción**:

```http
POST /api/v1/kiosk/checkins/scan-member-qr
X-CSRF-Token: <token-de-proteccion-csrf>
Idempotency-Key: <uuid-aleatorio-por-intento>
Content-Type: application/json
Cookie: <cookie-de-sesion-kiosco>

{"memberQr":"<credencial-firmada-efímera>"}
```

**Respuesta `200`**:

```json
{
  "allowed":false,
  "reasonCode":"duplicate",
  "message":"Ya existe un acceso registrado para hoy.",
  "occurredAt":"2026-11-05T11:38:03-06:00",
  "visitDate":"2026-11-05"
}
```

El frontend del kiosco muestra **solo el resultado de la lectura actual**, no nombre, correo, teléfono, foto, planes, deudas ni el historial de otros miembros. Si la recepción necesita ver identidad/foto para verificación presencial, crear **una vista aparte protegida por rol de personal**, no ampliar la respuesta del kiosco público.

### 4.5 Estadísticas y consulta de eventos privados

**Agregar**:

```http
GET /api/v1/gyms/{gymId}/checkins/stats?from=2026-11-01&to=2026-11-05
```

- Solo owner/trainer del gimnasio, bajo módulo Check-in habilitado; admin global autorizado por servicio.
- `from`/`to` son fechas **locales** del gimnasio (inclusive), límites y ventana máxima configurables.
- `total=granted+denied` calculado sobre registros reales, excluidos borrados lógicos; `byHour` mide **entradas/intent os**, no presencia.
- No devolver `occupancy`, `peopleInside`, `liveCapacity` ni valores simulados.

**Ejemplo `200`**:

```json
{
  "from":"2026-11-05",
  "to":"2026-11-05",
  "timezone":"America/Mexico_City",
  "total":18,
  "granted":15,
  "denied":3,
  "byHour":[{"hour":"07:00","granted":7,"denied":1}]
}
```

`byHour` es opcional si inicialmente solo se implementan totales; si se incluye, garantizar zona horaria local y agregados reales (sin agrupar sobre lista limitada). El `GET /dashboard/summary` actual **ya cuenta** entradas concedidas/denegadas; no duplicar su lógica: extraer una consulta común a `ICheckinRepository` y reutilizarla desde el dashboard si es viable sin alterar su JSON.

**Evolución no disruptiva de** `GET /gyms/{gymId}/checkins`: preservar el contrato de lista vigente y su `limit`; de ser necesario, añadir `offset`, `from`, `to` opcionales, sin cambiar el formato actual hasta que el frontend adopte un DTO explícito. `GET .../checkins/me` conserva `{"items": [...]}`. Nombres legibles son datos opcionales solo para consultas privadas autorizadas, nunca una razón para exhibir UUID.

### 4.6 Respuestas y códigos de error estables

| Estado | Uso |
|---|---|
| `200` | Consulta o intento de acceso procesado, tanto concedido como denegado de negocio. |
| `201` | Alta de dispositivo; POST manual **continúa devolviendo 201** como antes. |
| `204` | Logout kiosco, borrado lógico de dispositivo y activación mediante cookie. |
| `400` | Payload, fechas, QR malformado, clave de idempotencia faltante. |
| `401` | JWT de usuario ausente/inválido o sesión de dispositivo ausente/caducada. |
| `403` | Rol insuficiente, módulo deshabilitado, dispositivo deshabilitado o gimnasio no autorizado. |
| `404` | Gimnasio no encontrado/no público, dispositivo no perteneciente al gimnasio, recurso inexistente. |
| `409` | Reutilizar `Idempotency-Key` con payload diferente, conflictos de activación o colisión de concurrencia no recuperable. |
| `429` | Exceso de intentos, con `Retry-After`. |
| `500/503` | Fallo servidor/servicio, no convertir en `allowed:false` silencioso. |

Formato técnico de errores compatible con `ApiExceptionHandler`: `{"error":"stable_code","message":"Mensaje legible"}`. No devolver trazas ni datos internos.

## 5. Modelo de acceso: una sola política de negocio

### 5.1 Flujo de evaluación

```text
Verificar identidad (usuario o dispositivo) y gym
    ↓
Comprobar gimnasio activo + módulo habilitado
    ↓
Verificar token QR (hash/firma/propósito/gym/expiración)
    ↓
Resolver miembro en ESE gym (y no borrado)
    ↓
Evaluar cuenta/miembro/suscripción/pago desde backend
    ↓
Consultar acceso concedido previo para fecha local
    ↓
Registrar evento permitido o denegado con método correcto
    ↓
Devolver respuesta de negocio estable
```

**Fuente de verdad:** reutilizar y endurecer `v_member_access`, o reemplazar su consulta dentro de `IMemberAccessRepository`, pero mantener **una** implementación centralizada usada por check-in manual y ambos QR. No confiar en `useMemberStore.getAccessStatus()` ni en datos de pagos parciales del cliente.

**Códigos de negocio sugeridos, sin confundirlos:** `granted`, `duplicate`, `gym_inactive`, `member_inactive`, `user_inactive`, `no_active_subscription`, `no_payment`, `expired`, `unknown_validity`, `wrong_gym`, `token_expired`, `invalid_token`, `device_inactive`, `not_found`. Mantener los códigos actuales que ya se usan (por ejemplo `allowed`, `not_found`) como mapeo de compatibilidad hasta acordar DTO final. Evitar inventar el estado de pago en el frontend.

**Punto a corregir:** el `CASE` actual de `v_member_access` compara `next_due_date < hoy`; si `next_due_date IS NULL`, esa comparación no es verdadera. Definir que `NULL` **no concede acceso automáticamente**, salvo regla explícita y verificable de vigencia ilimitada/cortesía. Agregar condición y tests antes de permitir QR. Revisión de contratos con pagos para no romper membresías legítimas; no ejecutar una sustitución ciega de la vista.

**Política vigente de duplicados:** máximo un check-in concedido por miembro y por fecha **local** de gimnasio, acorde con `ux_member_daily_entry`. Cambiarla a «reingresos múltiples» requeriría una decisión posterior y migración distinta; **no cambiar** el índice en esta implementación.

**Eventos denegados:** se persisten para consultas privadas autorizadas. No ligar un UUID de miembro que no existe; el esquema permite `member_id IS NULL` si `allowed=false`. El campo `method` se determina en el servidor: `manual`, `kiosk_qr` o `member_qr`, nunca libre desde el cliente.

## 6. Migración Flyway propuesta (V5) y reutilización de tablas

**No modificar V1, V2, V3 o V4 existentes.** Agregar `src/main/resources/db/migration/V5__checkin_device_security_and_idempotency.sql` (nombre sujeto a la convención actual). Usar siempre `${schema_db}` — el esquema configurado actualmente es `test`, no `gym`.

| Estructura | Ya existe | Acción V5 |
|---|---|---|
| `${schema_db}.checkin_devices` | Sí | Reutilizar; añadir metadatos de activación (`activation_expires_at`, `activated_at`) si se elige el flujo de código de un solo uso. `device_secret_hash` siempre es hash, nunca secreto plano. |
| `${schema_db}.checkin_qr_tokens` | Sí | Reutilizar `token_hash`, `device_id`, `issued_at`, `expires_at`; agregar índices solo después de medir consultas. |
| `${schema_db}.checkin_events` | Sí | Reutilizar `method`, `device_id`, `allowed`, razones y restricciones. Añadir `idempotency_key_hash` y `request_fingerprint` opcionales con índice único parcial entre filas vigentes. |
| `${schema_db}.checkin_device_sessions` | No | Crear tabla de **sesiones del kiosco**: `id`, `gym_id`, `device_id`, `session_token_hash` (único), `expires_at`, `revoked_at`, `last_seen_at` y seis columnas de auditoría/borrado lógico. FK compuesta `(gym_id,device_id)` al dispositivo. |
| `v_member_access` | Sí | `CREATE OR REPLACE VIEW` **solo si las reglas de acceso acordadas lo exigen**, con salvaguarda para vigencia nula; nunca editar V4. |
| Nueva tabla de member-QR | No | **No necesaria** en el diseño elegido: credencial firmada de corta vida + verificación en BD. Si se requiere revocación individual instantánea de QR emitidos, documentar extensión de jti/revocación. |
| Tabla de notificaciones | No | **No crear**. |
| Tabla de clase muestra | `guest_passes` ya existe, pero no se usa para este alcance | **No crear ni exponer APIs** de invitado por ahora. |

**Índices/constraints obligatorios:** sesiones únicas por `session_token_hash`, FK del dispositivo por el **mismo gym**, caducidad válida; `ux_member_daily_entry` sin cambios; idempotencia única por gimnasio/clave de solicitud y fingerprint verificable; exclusión explícita de `deleted_at IS NULL` cuando aplique. El índice de sesiones debe poder encontrar una cookie por hash sin revelar el secreto.

**Auditoría interna:** tablas nuevas incluyen `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`, usando defaults y la estrategia existente. Para acciones de dispositivo sin usuario autenticado, `created_by` puede ser `NULL` y debe registrarse `device_id`/origen en las tablas de dominio correspondientes. No inventar un usuario global ficticio. No exponer estos campos en la guía de frontend ni en las respuestas públicas.

**Borrado lógico:** la desactivación no equivale a eliminar; `DELETE` de dispositivo marca `deleted_at` y `deleted_by` y revoca sesiones/tokens. Todos los repositorios usan `deletedAt IS NULL` (y `expiresAt > now` en tokens/sesiones). **No usar `em.remove()`, `DELETE FROM` Java, ni cascadas físicas para esta operación**. Tokens expirados se pueden dejar caducar por predicado y aplicar una política de archivo definida, sin romper la trazabilidad.

**Migración segura:** ejecutar sobre respaldo en DB de pruebas; revisar índices duplicados/constraints, `ddl-auto: validate`, tablas previas existentes y la compatibilidad del esquema `test` con los placeholders; verificar que V5 no falla al tener sesiones/filas previas. No hacer cambios destructivos.

## 7. Plan de clases y archivos por capa

| Archivo/clase | Acción | Motivo |
|---|---|---|
| `modules/checkin/routes/CheckinController.java` | **Modificar** solo para agregar `scan-gym-qr`, `me/qr`, `stats`; conservar los tres métodos actuales. | No romper contratos anteriores. |
| `modules/checkin/routes/KioskController.java` | **Crear** | Activación, sesión y operaciones del dispositivo; no confundir con actor JWT usuario. |
| `modules/checkin/routes/CheckinDeviceController.java` | **Crear** | CRUD administrativo de dispositivos del gym. |
| `modules/checkin/services/CheckinService.java` | **Refactor focalizado** | Extraer evaluación común; registrar métodos correctos, idempotencia y concurrencia sin tocar respuesta manual. |
| `modules/checkin/services/CheckinQrService.java` | **Crear** | Emisión/validación QR, expiración y propósito. |
| `modules/checkin/services/KioskDeviceService.java` | **Crear** | Provisión/activación/revocación de dispositivos/sesiones. |
| `modules/checkin/services/MemberAccessPolicy.java` | **Crear** | Única regla de autorización de membresía y duplicado entre flujos. |
| `modules/checkin/interfaces/ICheckinRepository.java` | **Ampliar** | Registrar método/dispositivo, query transaccional, estadísticas, idempotencia; conservar métodos existentes durante transición. |
| `modules/checkin/repositories/CheckinRepository.java` | **Ampliar** | JPQL/Criteria, bloqueo/consulta y persistencia; no SQL directo. |
| `modules/checkin/model/CheckinDevice.java` | **Crear** | JPA de `checkin_devices`: **extiende `AuditedRecord`, implementa `BaseEntity`**, mapea el `gymId` y usa `@SQLRestriction("deleted_at IS NULL")`. |
| `modules/checkin/model/CheckinQrToken.java` | **Crear** | JPA de `checkin_qr_tokens`, campos de expiración; **extiende `AuditedRecord`, implementa `BaseEntity`** y usa `@SQLRestriction("deleted_at IS NULL")`. |
| `modules/checkin/model/CheckinDeviceSession.java` | **Crear** | JPA de nueva tabla V5: hash y revocación; **extiende `AuditedRecord`, implementa `BaseEntity`** y usa `@SQLRestriction("deleted_at IS NULL")`. |
| `modules/checkin/interfaces/I*Repository` | **Crear** por entidad | Contratos de persistencia y búsquedas especializadas sin exponer JPA a Services. |
| `modules/checkin/repositories/*Repository.java` | **Crear** | Implementar contratos con `BaseCRUD` donde encaje; JPQL/Criteria tipados para sesiones/tokens. |
| `modules/checkin/dto/*Request.java`, `*Response.java` | **Crear** | DTO con `@Valid`, tipos y validaciones. Evitar `Map<String,Object>` en nuevos endpoints. |
| `security/DeviceAuthenticationFilter.java`, `DeviceAuthorizationService.java` | **Crear** | Principal distinto de usuario; seguridad de cookie/CSRF, gym y revocación. |
| `security/SecurityConfig.java` | **Modificar de forma acotada** | Enrutar autenticación de kiosco, exposiciones públicas mínimas, CORS/CSRF. Proteger rutas privadas existentes. |
| `shared/factories/ServiceFactory.java` | **Modificar** | Inyección de nuevos servicios siguiendo factoría actual. |
| `modules/dashboard/repositories/DashboardRepository.java` | **Revisar** sin alterar JSON | Reutilizar agregados de check-in, no introducir cálculos en cliente. |
| `db/migration/V5__...sql` | **Crear** | Compatibilidad, sesiones e idempotencia. |
| `src/test/java/com/gymback/checkin/*Test.java` | **Crear** | Contratos, reglas, concurrencia, permisos y aislamiento. |
| `docs/CONTRATOS_FRONTEND.md` y OpenAPI | **Actualizar** | Dar a Vue únicamente los nuevos contratos visibles, sin exponer auditoría interna. |

**Regla:** no cambiar nombres ni rutas de los controladores de pagos, miembros, gimnasios, landing, multimedia, staff ni auth para completar este módulo. Si alguna integración necesita datos adicionales, añadir campos opcionales compatibles y pruebas de regresión.

## 8. Comportamiento específico por rol y tenant

| Actor | Puede emitir QR gym | Puede pedir su QR miembro | Puede validar QR miembro en dispositivo | Puede administrar dispositivos | Puede ver historial privado |
|---|---|---|---|---|---|
| Visitante anónimo | No | No | No | No | No |
| Dispositivo autorizado | Sí, para **su gym** | No | Sí, para **su gym** | No | **No**: solo resultado actual |
| Miembro activo autenticado | No | Sí, de **su gym** | No | No | Solo `/checkins/me` |
| Entrenador autorizado | No (salvo sesión de dispositivo) | No como otro miembro | Solo mediante dispositivo autorizado o acción manual ya existente | No | Sí, del gym y según módulo |
| Propietario del gimnasio | No (salvo sesión de dispositivo) | No como otro miembro | Mediante dispositivo autorizado o acción manual | Sí, solo su gym | Sí, solo su gym |
| Admin global | No (salvo sesión de dispositivo) | No como otro miembro | Mediante dispositivo autorizado o flujo manual existente | Sí | Según autorización administrativa |

**Cruzamiento de gimnasios:** QR emitido en gimnasio A nunca valida entrada en B; un dispositivo A no emite para B; los miembros multi-gym son verificados para el gimnasio que aparece en el QR, no para su gimnasio por defecto. Suspender gym, deshabilitar módulo o borrar/inhabilitar dispositivo invalida sus operaciones desde la siguiente petición.

## 9. Idempotencia, concurrencia y consistencia de eventos

1. Las nuevas operaciones de validación aceptan `Idempotency-Key` UUID aleatorio obligatorio (o formato opaco de longitud acotada); valor diferente por intención de acceso, igual durante reintentos de red.
2. En servidor se almacena **hash** de `gym + actor/contexto + idempotencyKey` y fingerprint del payload; nunca almacenar/mostrar la clave en claro.
3. Si llega la misma clave y mismo payload, devolver **la misma decisión y el mismo evento**, sin registrar un segundo acceso.
4. Si llega la misma clave con payload distinto, `409` sin hacer otra operación.
5. Para solicitudes paralelas con claves diferentes, bloqueo pesimista sobre el miembro o exclusión adecuada en la transacción **más** el índice único `ux_member_daily_entry` como defensa final. No confiar solo en `alreadyAllowed()`.
6. Si ya hay entrada concedida hoy, responder `allowed:false`, `reasonCode:"duplicate"` y registrar denegación según política del módulo; no insertar segunda entrada `allowed=true`.
7. Garantizar que el actor autenticado/dispositivo y la razón de acceso se calculan en servidor. Una falla de BD devuelve error real, no una confirmación local ficticia.
8. Las consultas y conteos excluyen `deleted_at IS NOT NULL`; las entradas concedidas no se borran físicamente para permitir una segunda entrada indebida.

## 10. Verificación de lo demás sin ampliar alcance

### 10.1 Landing y multimedia

- Verificar contra el backend `GET /api/v1/public/gyms/{slug}/landing` para gimnasio activo y `404` para suspendido/archivado o slug inexistente; la página se publica automáticamente.
- Confirmar `/landing/link`, `/landing/slug`, `/landing/logo`, `landing-blocks`, `settings` y URL de imágenes públicas; `publicUrl` debe llevar al sitio Vue `/g/{slug}`.
- No crear `publish`, `unpublish`, `published`, ni reintroducir la tarjeta adicional de estado editorial.
- La ruta raíz `/` y CTA del Hero son decisión de frontend/producto; no crear API pública inventada por defecto.

### 10.2 Sesiones y cuentas

- `GET /auth/me` **sí existe** y devuelve `user` y `gyms`. Agregar pruebas de contrato/respuesta y autorización; no reemplazarlo.
- Login, refresh rotativo y logout se mantienen. Persistencia después de F5 mediante cookie HttpOnly **no está aprobada para la sesión de usuario** en este alcance; documentar la limitación actual y tratarla en tarea separada de seguridad.
- Mantener `POST /staff/register` para entrenador; alta de owner mediante `POST /users` y luego `POST /staff`, según bandera `USER_API_ENABLED`; no crear endpoint alterno sin causa.
- Revisar 403/404/409/204 y ausencia de datos de otro gimnasio, sin tocar roles globales.

### 10.3 Clase muestra, ocupación y notificaciones

- **Clase muestra:** pantalla informativa, sin API nueva; no consumir `guest_passes` ni `reservations` desde visitante.
- **Ocupación:** solo conteo real de entradas/denegaciones, por fecha local; sin presentar aforo en vivo ni «0 personas presentes».
- **Notificaciones:** siguen locales al frontend; no crear `NotificationController`, repositorios ni persistencia. Corregir en Vue títulos con IDs de venta, no en el backend.

## 11. Matriz de pruebas de aceptación obligatoria

| ID | Caso | Resultado esperado |
|---|---|---|
| QR-01 | Owner crea dispositivo en gimnasio A | 201, código de activación temporal visible una vez. |
| QR-02 | Visitante intenta emitir token QR sin dispositivo | 401/403, nunca se emite QR válido. |
| QR-03 | Activación con código válido/expirado/reutilizado | Solo válido una vez; fallos uniformes y limitados. |
| QR-04 | Dispositivo revocado intenta operar con cookie previa | Rechazo inmediato; sin QR ni check-in. |
| QR-05 | Kiosco A intenta operar con gym B | 403 o respuesta segura; sin evento concedido. |
| QR-06 | Miembro activo escanea QR gimnasio dentro de vigencia | Un evento `allowed=true`, `method=kiosk_qr`. |
| QR-07 | QR gimnasio expirado, manipulado o de gym distinto | No concede; razón estable, no filtra datos. |
| QR-08 | Miembro genera QR personal y dispositivo válido lo escanea | Un evento concedido, `method=member_qr`. |
| QR-09 | QR personal vencido, firma alterada, propósito incorrecto | No concede; sin excepción 500. |
| QR-10 | `GMS-MEMBER:<code>` como credencial | Rechazado; no basta para ingresar. |
| QR-11 | Miembro sin pago, vencido, inactivo, sin plan, `next_due_date=NULL` | Misma decisión para manual y ambos QR; **nunca conceder por datos incompletos**. |
| QR-12 | Dos peticiones simultáneas al mismo gym/miembro/día | **Solo un** `allowed=true` protegido por índice y transacción. |
| QR-13 | Retry con misma `Idempotency-Key` | Mismo resultado; no duplica evento. |
| QR-14 | Retry misma clave con distinto QR/body | 409; no registra entrada. |
| QR-15 | Cambio a gym suspendido/módulo deshabilitado durante sesión | Rechazo desde siguiente operación. |
| QR-16 | Apertura anónima de kiosco y lectura de registros | Solo información pública mínima; no hay nombres/PII/historial. |
| QR-17 | `GET /checkins/me` de usuario A intenta leer B | Solo accesos propios y del gimnasio autorizado. |
| QR-18 | Métricas `from/to` abarcan fin de mes y cambio horario | Conteos exactos y fecha/hora del timezone del gimnasio. |
| QR-19 | Rol trainer/owner/admin y autorización dispositivos | Matriz de permisos respetada; tenant aislado. |
| QR-20 | Soft delete de dispositivo y sesiones | No visibles en GET ni usables para autenticar; sin borrado físico. |
| REG-01 | POST manual existente | Misma ruta/payload/status `201`/respuesta; todavía escribe `manual`. |
| REG-02 | GET checkins y GET checkins/me existentes | Contratos JSON previos intactos y no muestran otros gyms. |
| REG-03 | Dashboard/summary y pagos | JSON y agregados previos intactos; sin duplicar cálculos. |
| REG-04 | Login, refresh, logout, auth/me | Sin regresiones ni bucles de sesión. |
| REG-05 | Owner/trainer nuevos y existentes | Flujos `/users`, `/staff`, `/staff/register` intactos. |
| REG-06 | Landing por slug, logo, bloques, imágenes | Sin publicación manual; activo visible, suspendido 404. |
| REG-07 | V5 con BD que ya tiene V1–V4 | Flyway pasa, `ddl-auto=validate`, repositorios JPA funcionan. |
| REG-08 | Correo, notificaciones y clase muestra | No aparecen endpoints/tablas nuevas de estos módulos. |

## 12. Entregables y criterios de definición de terminado

### Backend

1. Código refactorizado y **completo** (no fragmentos), con nuevas clases por capa y sin modificar contratos existentes.
2. Una única migración **V5 no destructiva** compatible con esquema `test` y con soft delete/auditoría de tablas nuevas.
3. OpenAPI/Swagger + colección Postman o HTTP examples con permisos, ejemplos, errores, expiraciones, `Idempotency-Key` y cookies/CSRF.
4. Pruebas unitarias de política/QR y pruebas de integración reales con PostgreSQL (idealmente Testcontainers); pruebas de seguridad multi-gym, multirrol y concurrencia.
5. `mvn clean verify` exitoso con POM real; arranque Spring Boot + Flyway V5 + prueba de `/auth/me`, `/checkins` manual y ambos QR. Si no se ejecuta algo, reportarlo, **no** afirmar que pasó.
6. Guía breve de despliegue: HTTPS, origen frontend, cookies, CSRF, rotación de claves, variables de entorno, revocación y activación de dispositivos.
7. Guía frontend actualizada **solo con interfaces y contratos funcionales**. No presentar campos o pantallas de auditoría técnica.

### Orden de ejecución

**Etapa 1 — compatibilidad:** tests de contratos actuales, captura de JSON y validación de `v_member_access`.

**Etapa 2 — persistencia:** V5, entidades JPA, interfaces, sesiones, dispositivos, hashes y filtros de borrado.

**Etapa 3 — seguridad:** activación, filtro/principal de kiosco, cookie+CSRF, revocación, límites de abuso.

**Etapa 4 — negocio:** QR gimnasio, QR miembro, servicio común de acceso, métodos/eventos, idempotencia y transacciones.

**Etapa 5 — estadísticas + entrega:** agregados reales, OpenAPI, pruebas regresión/E2E, documentación frontend y despliegue.

### Criterio de aceptación final

Se considera terminado solo cuando: **(a)** ambos flujos QR registran accesos reales, **(b)** un kiosco anónimo no puede registrar ni consultar datos privados, **(c)** ningún gimnasio accede al de otro, **(d)** expiración, duplicado y pago se resuelven en servidor, **(e)** auditoría/borrado lógico siguen vigentes, **(f)** el frontend no requiere cambiar endpoints antiguos y **(g)** las pruebas pasan contra PostgreSQL.

---

### Anexo: pautas para el frontend (resumen, no documento interno)

- `/checkin?gym=<slug>` mantiene el gimnasio de contexto. Solo la sesión de kiosco habilita emisión y escaneo; no mostrar tokens como texto al usuario.
- Miembro escanea QR del gimnasio: `POST /gyms/{gymId}/checkins/scan-gym-qr` con Bearer y clave de idempotencia.
- QR del miembro: `POST /gyms/{gymId}/checkins/me/qr` con Bearer; recepción lo valida mediante `POST /kiosk/checkins/scan-member-qr` con cookie+CSRF.
- No usar el QR local `GMS-MEMBER:<code>` como autorización; no declarar exitoso un check-in si la API no lo confirmó.
- No mostrar nombres, identificadores, pagos, tokens ni últimos accesos en el kiosco anónimo.
- «Clase muestra» permanece informativa, no agregar formularios que aparenten registrar; las notificaciones siguen locales y la ocupación en vivo no está disponible.
- Landing activa automáticamente por slug; sin controles publicar/despublicar.

**Fin de especificación.**
