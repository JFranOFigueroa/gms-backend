# GMS · Integración frontend de Check-in por QR y kiosco

> Esta guía describe los contratos de los endpoints añadidos al backend. No modifica el flujo manual ni las demás pantallas existentes. Los IDs se usan únicamente para peticiones y estado interno: **no se imprimen como texto visible**.

## 1. Alcance confirmado

- **Dos modos:** el miembro escanea el QR del gimnasio; recepción escanea el QR temporal del miembro.
- `/checkin?gym=<slug>` identifica el gimnasio solo para mostrar su identidad pública. **No autoriza accesos** por estar abierta la URL.
- Recepción debe trabajar en un dispositivo activado por owner o administrador global.
- Clase muestra se mantiene informativa; las notificaciones siguen siendo locales; el dashboard solo ofrece conteos reales de entradas y rechazos, **no aforo en vivo**.
- La landing `/g/:slug` sigue disponible automáticamente para gimnasios activos; **no se agregan botones de publicar/despublicar**.
- Nunca usar `GMS-MEMBER:<code>` como credencial de acceso. No usar contraseña del usuario en un kiosco.

## 2. Acceso de la aplicación y configuración

API base: `/api/v1`. Todas las solicitudes privadas siguen usando `Authorization: Bearer <accessToken>`.

Para `/kiosk/activate`, `/kiosk/session`, `/kiosk/qr/issue`, `/kiosk/logout` y `/kiosk/checkins/scan-member-qr` se usa **cookie de sesión de dispositivo**, no Bearer. En fetch/Axios usar `credentials: 'include'` / `withCredentials: true`. Se requiere frontend y API del mismo *site* para `SameSite=Strict`, API HTTPS y `FRONTEND_ORIGIN` exacto.

En las operaciones POST de kiosco **después de activar** agregar `X-CSRF-Token` obtenido de `GET /kiosk/session`. El navegador envía el encabezado `Origin` automáticamente; el backend exige que coincida con `FRONTEND_ORIGIN`. No almacenar ni enseñar cookies, QR en claro ni claves.

## 3. Dispositivos (pantalla administrativa privada)

Solo owner del gimnasio y admin global, siempre que el módulo Check-in esté habilitado.

| Acción | Método y ruta | Cuerpo / retorno |
|---|---|---|
| Lista | `GET /gyms/{gymId}/checkin-devices` | Lista `{deviceId, deviceName, active, activated, lastSeenAt}` |
| Crear | `POST /gyms/{gymId}/checkin-devices` | `{ "deviceName": "Recepción principal" }` → `201`, incluye `activationCode` y `activationExpiresAt` |
| Regenerar código | `POST /gyms/{gymId}/checkin-devices/{deviceId}/reissue-activation` | `200`, nuevo código. Revoca sesiones previas |
| Activar/desactivar | `PATCH /gyms/{gymId}/checkin-devices/{deviceId}` | `{ "active": false }` o `true` |
| Eliminar | `DELETE /gyms/{gymId}/checkin-devices/{deviceId}` | `204`; no vuelve a aparecer ni puede operar |

El **código de activación** se muestra **una sola vez**. Dura 10 minutos. Si se desactiva/revoca un kiosco, no recupera su sesión anterior. Para volver a usarlo tras desactivación o expiración es necesario habilitarlo y generar otro código.

## 4. Kiosco público autorizado

1. Al entrar a `/checkin?gym=<slug>`, llamar `GET /public/gyms/{slug}/checkin-config` **sin Bearer**. Responde `{ "name": "Centro Neuro", "slug": "centro-neuro", "checkinAvailable": true }`. Slug inexistente o gimnasio no activo → `404`.
2. Intentar `GET /kiosk/session` con `withCredentials: true`. Si devuelve `401`, presentar pantalla de activación (sin aceptar accesos todavía).
3. `POST /kiosk/activate` con `{ "slug": "centro-neuro", "activationCode": "<código-temporal>" }`, `withCredentials: true`. Respuesta `204` + cookie `HttpOnly`; no guardar el código de activación.
4. Repetir `GET /kiosk/session`. Respuesta `{ "gymName": "Centro Neuro", "slug": "centro-neuro", "deviceName": "Recepción principal", "expiresAt": "<ISO>", "csrfToken": "<valor>" }`. Conservar `csrfToken` solo en memoria.
5. Para salir: `POST /kiosk/logout` con cookie + `X-CSRF-Token`, devuelve `204`.

No mostrar listados de personas, correos, pagos, últimos accesos ni historial en la zona pública. Tras 401/403 suspender el escáner y pedir reactivación si corresponde.

## 5. Flujo A: miembro escanea el QR del gimnasio

**En el kiosco:**

```http
POST /api/v1/kiosk/qr/issue
Origin: <FRONTEND_ORIGIN del navegador>
X-CSRF-Token: <csrfToken de GET /kiosk/session>
Cookie: <enviada automáticamente por el navegador>
```

`200`:

```json
{
  "qrValue": "<token-opaco>",
  "expiresAt": "2026-11-05T18:00:10Z",
  "serverTime": "2026-11-05T18:00:00Z",
  "refreshAfterSeconds": 8
}
```

Renderizar **como imagen QR**, nunca como texto de la UI. Solicitar uno nuevo antes de vencer; usar `expiresAt` y `serverTime` como referencia, no generar tokens en JavaScript. El QR está asociado al gimnasio y al dispositivo autorizado (TTL configurado: 5–300 segundos, por defecto 10).

**En la cuenta autenticada del miembro:**

```http
POST /api/v1/gyms/{gymId}/checkins/scan-gym-qr
Authorization: Bearer <accessToken>
Idempotency-Key: <UUID-nuevo-por-intento>
Content-Type: application/json

{ "kioskToken": "<qrValue-escaneado>" }
```

`200` de negocio, concedido o rechazado:

```json
{
  "allowed": true,
  "reasonCode": "granted",
  "message": "Acceso registrado correctamente.",
  "occurredAt": "2026-11-05T18:00:05Z",
  "visitDate": "2026-11-05"
}
```

Si el dispositivo genera un QR de otro gimnasio o uno vencido, el backend responde `allowed:false`. No indicar éxito antes de recibir respuesta. En un reintento de **la misma acción** usar **la misma** `Idempotency-Key` y el mismo contenido.

## 6. Flujo B: recepción escanea QR del miembro

**El miembro autenticado** obtiene su credencial de 30 segundos:

```http
POST /api/v1/gyms/{gymId}/checkins/me/qr
Authorization: Bearer <accessToken>
```

`200`:

```json
{
  "qrValue": "<credencial-firmada>",
  "expiresAt": "2026-11-05T18:00:30Z",
  "serverTime": "2026-11-05T18:00:00Z",
  "refreshAfterSeconds": null
}
```

En el dispositivo autorizado, leer ese QR con cámara HTTPS y enviar:

```http
POST /api/v1/kiosk/checkins/scan-member-qr
X-CSRF-Token: <csrfToken>
Idempotency-Key: <UUID-nuevo-por-intento>
Content-Type: application/json
Cookie: <sesión de kiosco enviada por el navegador>

{ "memberQr": "<credencial-firmada>" }
```

Ejemplo de rechazo válido (`200`):

```json
{
  "allowed": false,
  "reasonCode": "duplicate",
  "message": "Ya existe un acceso registrado para hoy.",
  "occurredAt": "2026-11-05T18:00:03Z",
  "visitDate": "2026-11-05"
}
```

En el kiosco mostrar solamente ese resultado y no mostrar identidad o información financiera del miembro. Si no hay cámara, mostrar indisponibilidad; no crear accesos locales ficticios.

## 7. Historial propio, registro manual y estadísticas (sin romper contratos)

Se conservan:

```http
GET  /gyms/{gymId}/checkins?limit=50
GET  /gyms/{gymId}/checkins/me?limit=50
POST /gyms/{gymId}/checkins
{ "memberId": "<UUID-interno>" }
```

El `POST` manual sigue devolviendo `201` con `{eventId, allowed, reason, visitDate}`.

Nueva consulta privada para owner/trainer/admin:

```http
GET /gyms/{gymId}/checkins/stats?from=2026-11-01&to=2026-11-05
```

`200`:

```json
{
  "from": "2026-11-01", "to": "2026-11-05", "timezone": "America/Mexico_City",
  "total": 18, "granted": 15, "denied": 3,
  "byHour": [{ "hour": "07:00", "granted": 7, "denied": 1 }]
}
```

Son **entradas e intentos** por hora, no personas actualmente dentro. Rango máximo: 32 fechas inclusivas. `byHour` omite las horas sin intentos.

## 8. Manejo de estados y mensajes

- `200` con `allowed:false`: decisión de negocio correcta; mostrar `message`, no tratarla como HTTP fallido.
- `201`: dispositivo creado o check-in manual registrado.
- `204`: activación/logout/borrado satisfactorio, sin cuerpo.
- `400`: datos inválidos, `Idempotency-Key` faltante o no UUID.
- `401`: usuario no autenticado o cookie de kiosco vencida/ausente.
- `403`: rol, gimnasio, módulo, dispositivo u origen no autorizado.
- `404`: recurso o slug no disponible.
- `409`: conflicto de clave de reintento con otro contenido o nombre de dispositivo repetido.
- `429`: límite de intentos, usar `Retry-After`.
- `5xx`: problema de servidor; no fingir operación exitosa.

Códigos de negocio más comunes: `granted`, `duplicate`, `expired`, `no_payment`, `unknown_validity`, `no_active_subscription`, `member_inactive`, `wrong_gym`, `invalid_token`, `token_expired`, `device_inactive`.

**No cambiar** textos ni rutas de otras pantallas; solo añadir controles funcionales del lector QR y sesión de dispositivo. Detener y liberar `MediaStreamTrack`s al salir o al confirmar una lectura; solicitar permiso de cámara solo bajo HTTPS/localhost. Limpiar datos de gimnasio anterior al cambiar de contexto.

## 9. Estado de integración y pruebas necesarias

Estas rutas están implementadas en el código entregado, pero todavía **no se han probado de extremo a extremo** contra PostgreSQL ni se ha compilado con el POM real. Antes de considerar el frontend conectado, verificar autenticación, sesiones, caducidad, cambio de gimnasio, duplicados concurrentes, CORS/cookie y escaneo con una base de pruebas real.

## 10. Complemento V6: código manual y nombres legibles

Consultar **`docs/GUIA_FRONTEND_CHECKIN_CODIGO_Y_SESION.md`** para el contrato de las dos rutas nuevas. `GET /kiosk/session` añade `slug` y `GET /gyms/{gymId}/checkins` añade nombres legibles. Los endpoint QR, sus respuestas y el `POST /checkins` administrativo no cambian.
