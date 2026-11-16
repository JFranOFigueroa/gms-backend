# Contratos de backend para el frontend

La fuente de verdad es el código actual. Este documento resume los contratos que consume GMS Frontend.

## Autenticación

### `POST /api/v1/auth/login`

```json
{ "email": "admin@gms.local", "password": "..." }
```

Respuesta:

```json
{
  "token": "...",
  "tokenType": "Bearer",
  "user": {
    "id": "...",
    "name": "Administrador",
    "email": "admin@gms.local",
    "platformAdmin": true,
    "defaultBranchId": "..."
  },
  "branches": [
    { "id": "...", "name": "Sucursal Demo", "code": "sucursal-demo", "roles": ["owner"] }
  ],
  "refreshToken": "...",
  "expiresIn": 3600
}
```

- `platformAdmin` puede iniciar sesión sin sucursales.
- `branches[]` contiene solo sucursales activas autorizadas.
- `user.role` no existe; el rol se deriva de `platformAdmin` y `branches[].roles`.

### `GET /api/v1/auth/me`

Devuelve `{ user, branches }`.

### `POST /api/v1/auth/refresh`

```json
{ "refreshToken": "..." }
```

### `POST /api/v1/auth/logout`

```json
{ "refreshToken": "..." }
```

## Sitio

- `GET /api/v1/public/site` (anónimo).
- `GET|PATCH /api/v1/site/settings`.
- CRUD `/api/v1/site/landing-blocks`.
- CRUD `/api/v1/site/media` + `POST /upload`.
- `GET /api/v1/public/media/{id}` (anónimo).

La landing pública devuelve `{ site, theme, blocks, mediaAssets }`. No depende de sucursal.

## Sucursales

- `GET /api/v1/branches`
- `GET /api/v1/branches/{id}`
- `POST /api/v1/branches`
- `PATCH /api/v1/branches/{id}`
- `DELETE /api/v1/branches/{id}`

Solo `platformAdmin`.

## Operación por sucursal

Base: `/api/v1/branches/{branchId}`.

| Recurso | Ruta |
|---|---|
| Staff | `/staff` |
| Miembros | `/members` |
| Planes | `/plans` |
| Pagos | `/payments` |
| Clases | `/classes` |
| Plantillas | `/class-templates` |
| Reservas | `/reservations` |
| Rutinas | `/routines` |
| Productos | `/products` |
| Ventas | `/sales` |
| Check-ins | `/checkins` |
| Configuración | `/settings` |
| Módulos | `/modules` |
| Dispositivos | `/checkin-devices` |
| Dashboard | `/dashboard/summary` |

## Miembros

```http
POST /api/v1/branches/{branchId}/members
{ "userId": "UUID_EXISTENTE", "code": "M-001", "active": true }
```

No crea cuentas globales. Solo vincula un `userId` activo.

### Fotos privadas

- `GET /api/v1/branches/{branchId}/members/{memberId}/photo`
- `POST /api/v1/branches/{branchId}/members/{memberId}/photo`
- `DELETE /api/v1/branches/{branchId}/members/{memberId}/photo`

Requieren Bearer.

## Ventas

```http
GET  /api/v1/branches/{branchId}/sales
POST /api/v1/branches/{branchId}/sales
{ "items": [{ "productId": "UUID", "quantity": 2 }] }
```

Las respuestas incluyen `items[]` con `productId`, `productName`, `quantity`, `unitPrice` y `lineTotal`. Los precios los calcula el servidor.

## Check-in y kiosco

- `GET /api/v1/public/branches/{code}/checkin-config`.
- `POST /api/v1/kiosk/activate` con `{ code, activationCode }`.
- `GET /api/v1/kiosk/session`.
- `POST /api/v1/kiosk/logout`.
- `POST /api/v1/kiosk/qr/issue`.
- `POST /api/v1/kiosk/checkins/scan-member-qr`.
- `POST /api/v1/kiosk/checkins/validate-code`.

El kiosco usa `?branch=<code>` en el frontend. No existe landing por sucursal.

## Errores

El frontend espera JSON con `message`. Códigos:

- `400` datos inválidos.
- `401` sesión inválida.
- `403` sin permisos.
- `404` recurso inexistente.
- `409` conflicto de código/correo/idempotencia.
- `413` archivo demasiado grande.
