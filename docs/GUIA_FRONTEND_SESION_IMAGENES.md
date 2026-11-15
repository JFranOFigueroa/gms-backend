# Guía de sesión, imágenes y pantallas

## Sesión

1. `POST /api/v1/auth/login` valida credenciales.
2. El frontend muestra el selector de sucursal usando `branches[]`.
3. Un `platformAdmin` puede continuar sin sucursal.
4. `GET /api/v1/auth/me` restaura `{ user, branches }`.
5. `POST /api/v1/auth/refresh` rota el refresh token.

- Access token en `sessionStorage`.
- Refresh token solo en memoria.
- Sucursal activa en `sessionStorage` (`gymback:selected-branch`).
- `main.js` hidrata la sesión aunque no exista sucursal activa.

## Imágenes del sitio

- Catálogo: `GET /api/v1/site/media`.
- Subida: `POST /api/v1/site/media/upload` multipart `file` y `name`.
- Asset externo: `POST /api/v1/site/media` con `externalUrl` HTTPS.
- Entrega pública: `GET /api/v1/public/media/{id}`.
- Formatos: PNG, JPG, WebP; máximo 8 MB.
- El logo del sitio se guarda como `logoAssetId` en `site_settings`.

## Fotos de miembros

- Son privadas.
- Se solicitan con Bearer:
  - `GET /api/v1/branches/{branchId}/members/{memberId}/photo`.
- Se muestran como blob/object URL, nunca como `img.src` directo al endpoint privado.
- Se eliminan con `DELETE` sobre la misma ruta.

## Landing Builder

- Edita `site_settings` y `site_landing_blocks`.
- No tiene sucursal ni publicación.
- La vista previa usa los bloques ordenados.
- La landing pública consume `GET /api/v1/public/site`.

## Kiosco

- Ruta frontend: `/checkin?branch=<code>`.
- Configuración pública: `GET /api/v1/public/branches/{code}/checkin-config`.
- Activación: `POST /api/v1/kiosk/activate` con `{ code, activationCode }`.
- La cookie de dispositivo es HttpOnly.
- No se usa una landing por sucursal.

## Errores de UI

- Mostrar `message` del backend cuando sea legible.
- Evitar exponer nombres internos (`USER_API_ENABLED`, SQL, UUID) al usuario.
- Para conflictos de idempotencia, pedir revisar la operación antes de repetir.
