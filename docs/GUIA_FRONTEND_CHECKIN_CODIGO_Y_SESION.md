# GMS — Integración puntual de código manual y sesión de kiosco (V6)

**Base:** `DIAGNOSTICO_INTEGRACION_TOTAL_FRONTEND_GMS.md`, el backend V5 de QR y las decisiones actuales. **No modificar otras pantallas ni contratos de otros módulos.** Los dos lectores QR y el Check-in administrativo conservan sus rutas y JSON anteriores. No se agregan interfaces de auditoría técnica.

## 1. Funcionalidades disponibles

| Acción | HTTP | Autenticación | Estado |
|---|---|---|---|
| Solicitar código manual del propio miembro | `POST /api/v1/gyms/{gymId}/checkins/me/code` | JWT de miembro | **Nuevo V6** |
| Validar código en kiosco | `POST /api/v1/kiosk/checkins/validate-code` | Cookie `GMS_KIOSK` + `X-CSRF-Token` + `Origin` válido + `Idempotency-Key` | **Nuevo V6** |
| Identificar gimnasio en sesión del kiosco | `GET /api/v1/kiosk/session` | Cookie de kiosco | **Ampliado V6: `slug`** |
| Obtener historial privado y nombres | `GET /api/v1/gyms/{gymId}/checkins?limit=50` | JWT de personal autorizado | **Ampliado V6: nombres** |
| QR de gimnasio | `POST /api/v1/kiosk/qr/issue` + `POST /api/v1/gyms/{gymId}/checkins/scan-gym-qr` | Mecanismos anteriores | Sin cambios |
| QR del miembro | `POST /api/v1/gyms/{gymId}/checkins/me/qr` + `POST /api/v1/kiosk/checkins/scan-member-qr` | Mecanismos anteriores | Sin cambios |
| Check-in administrativo | `POST /api/v1/gyms/{gymId}/checkins` | JWT de owner/trainer/admin | Sin cambios |

**Importante:** los códigos manuales NO son `GMS-MEMBER:<member_code>` ni el código permanente del expediente. El miembro debe iniciar sesión y pedir un **código temporal de un solo uso**. Sirve cuando no puede usar la cámara, no sustituye la sesión del miembro ni la autorización del kiosco.

## 2. Miembro solicita código temporal

```http
POST /api/v1/gyms/{gymId}/checkins/me/code
Authorization: Bearer <accessToken>
```

Respuesta `200` (ilustrativa; tiempos emitidos por servidor):

```json
{
  "accessCode": "ABCD-EFGH-JKLM",
  "expiresAt": "2026-11-05T18:02:00Z",
  "serverTime": "2026-11-05T18:00:00Z"
}
```

Mostrar temporalmente el código al miembro en su pantalla autenticada, con cuenta regresiva desde los campos del servidor. No guardarlo en localStorage, logs, analítica o URL. Dura **120 segundos**, se invalida al usarse o pedir otro, con máximo **3 emisiones por minuto por miembro**. El backend retorna `Cache-Control: no-store`. En el frontend el código puede copiarse, pero debe limpiarse al salir/cambiar de gimnasio/cerrar sesión.

## 3. Kiosco valida el código introducido

**Precondiciones:** el dispositivo ya está activado mediante el flujo V5, `GET /kiosk/session` confirmó el gimnasio, y el frontend tiene `csrfToken` en memoria. No enviar Bearer del propietario/recepcionista. Usar `withCredentials: true`.

```http
POST /api/v1/kiosk/checkins/validate-code
X-CSRF-Token: <csrfToken_de_kiosk_session>
Idempotency-Key: <UUID_unico_por_intento>
Content-Type: application/json
Cookie: <enviada_por_navegador>

{"accessCode":"ABCD-EFGH-JKLM"}
```

Respuesta `200` **tanto si se concede como si se rechaza por negocio**:

```json
{
  "allowed": false,
  "reasonCode": "invalid_code",
  "message": "Código inválido, vencido o ya utilizado. Solicita uno nuevo.",
  "occurredAt": "2026-11-05T18:02:10Z",
  "visitDate": "2026-11-05"
}
```

Otros `reasonCode` ya existentes: `granted`, `duplicate`, `expired`, `no_payment`, `unknown_validity`, `no_active_subscription`, `member_inactive`, `user_inactive`. **No interpretar HTTP 200 como acceso concedido; usar `allowed`.** Durante un reintento de red del mismo envío conservar *la misma* `Idempotency-Key` y *el mismo* código; para un intento nuevo generar otro UUID. Desactivar el botón durante la petición.

- `400`: cuerpo o clave de idempotencia inválidos.
- `401`: cookie vencida/ausente; detener validación y solicitar activación.
- `403`: dispositivo/CSRF/Origin/módulo/gimnasio no autorizado.
- `409`: misma clave de idempotencia reutilizada con contenido diferente.
- `429`: superar 10 intentos por minuto por dispositivo; respetar `Retry-After`.

Los códigos se normalizan admitiendo mayúsculas/minúsculas, guiones y espacios; el código canónico contiene 12 caracteres de alta entropía. El kiosco **no recibe nombres, correos ni datos de pago del miembro** por esta operación.

## 4. Evitar usar un kiosco activado para otro gimnasio

`GET /api/v1/kiosk/session` conserva las propiedades existentes y añade `slug`:

```json
{
  "gymName": "Gimnasio Centro",
  "slug": "gimnasio-centro",
  "deviceName": "Recepción principal",
  "expiresAt": "2026-11-06T06:00:00Z",
  "csrfToken": "<csrfToken>"
}
```

Comparar `slug` con el slug canónico obtenido de `GET /api/v1/public/gyms/{slug}/checkin-config` al entrar desde `/checkin?gym=<slug>`. Si son diferentes, **no habilitar las acciones** ni reutilizar la sesión para el otro gimnasio. Pedir cierre de sesión de dispositivo/reactivación correspondiente. Esta comparación es una ayuda de UI; **el backend asocia todas las validaciones al gimnasio de la cookie**, no a un `gymId` controlado por el navegador.

## 5. Nombres legibles solo en consulta privada

`GET /api/v1/gyms/{gymId}/checkins?limit=50` conserva `id`, `member_id`, `staff_user_id`, `device_id`, `method`, `allowed`, `reason_code`, `visit_date`, `occurred_at`, y las demás claves anteriores. Añade:

- `member_name`: nombre del miembro vinculado si continúa disponible.
- `staff_name`: nombre del usuario cuando el evento corresponde a personal.
- `recorded_by_name`: nombre del empleado que registró el evento, si existe.
- `device_name`: nombre del dispositivo si está disponible.
- `display_name`: nombre legible prioritario o valor genérico (`Miembro`, `Personal`, `Invitado`, `Acceso sin identificar`).

En `src/api/mappers.js` / la pantalla privada, priorizar `display_name` y no mostrar nunca el UUID como fallback. **Esto no cambia `GET /checkins/me`, ni crea un listado público.** No mostrar auditoría técnica ni campos `*_by` al usuario.

## 6. Archivos del frontend a tocar (mínimos)

1. `src/api/endpoints.js`: registrar **solo las dos rutas nuevas**; las demás deben conservarse.
2. `src/api/service.js`: agregar métodos de emisión y validación (reutilizar cliente JWT y cliente kiosco de V5).
3. `src/stores/useCheckinStore.js`: guardar exclusivamente el código temporal en memoria y realizar las solicitudes reales.
4. `src/pages/dashboard/CheckInPage.vue`: permitir al miembro generar/ver su código temporal si la pantalla actual lo necesita.
5. `src/pages/public/StandaloneCheckInPage.vue`: conectar **Validar código** y verificar el `slug` de la sesión del kiosco.
6. `src/api/mappers.js` y/o `src/pages/dashboard/CheckInAuditPage.vue`: **solo** usar `display_name` de la consulta privada y dejar de mostrar UUID como texto.

No editar Landing Builder, login, usuarios, miembros, pagos, POS, calendario, rutinas, notificaciones ni diseños ajenos al Check-in. Mantener los textos existentes salvo el mensaje mínimo indispensable para código nuevo. Evitar hacer cambios en componentes de QR que ya funcionan.

## 7. Pruebas de integración obligatorias

- Código emitido por miembro correcto y kiosco del mismo gym: concede solo si su membresía está vigente.
- Mismo código usado dos veces con **claves diferentes**: solo el primer canje puede conceder; el segundo se rechaza.
- Reintento HTTP con **misma clave y mismo payload**: mismo resultado, sin nuevo evento.
- Reintento con **misma clave y distinto payload**: `409`.
- Código expirado (>120 segundos), revocado por nueva emisión, inválido o de otro gimnasio: rechazo.
- 10 intentos/minuto de un dispositivo: siguiente intento `429`.
- Sesión del kiosco con `slug` distinto del que aparece en URL: botones deshabilitados.
- Consulta privada del historial: nombres correctos; ninguna persona de otro gym; sin UUID visibles.
- QR de gimnasio, QR de miembro y registro manual administrativo: funcionan igual que antes; todos se contabilizan en `GET /checkins/stats`.

**Advertencia de despliegue:** aplicar Flyway **V6** antes de usar los endpoints nuevos. Las clases y pruebas se incluyeron en el ZIP, pero es obligatorio ejecutar `mvn clean verify` con el `pom.xml` real, migración y pruebas contra PostgreSQL.
