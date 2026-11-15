# Decisión vigente: una sola landing del sitio

## Alcance

- Existe **una única página pública** para toda la instalación.
- La landing representa a la marca/sitio, no a una sucursal.
- No existen rutas `/g/{slug}`, landings por sucursal ni publicaciones por gimnasio.
- El contenido se guarda en `site_landing_blocks`; la identidad y el tema en `site_settings`; las imágenes en `site_media_assets`.
- No hay flujo manual de publicar/despublicar: los cambios guardados se reflejan en la siguiente consulta.

## Contrato público

```http
GET /api/v1/public/site
```

Anónimo. Devuelve:

```json
{
  "site": {
    "name": "GMS Fitness",
    "shortName": "GMS",
    "logoUrl": "/api/v1/public/media/...",
    "contactEmail": "hola@gms.local",
    "contactPhone": "+52 55 0000 0000",
    "address": "Av. Principal 100",
    "timezone": "America/Mexico_City",
    "currency": "MXN"
  },
  "theme": { "preset": "crossfit-dark", "overrides": {} },
  "blocks": [
    { "id": "...", "type": "hero", "order": 0, "enabled": true, "data": {} }
  ],
  "mediaAssets": [
    { "id": "...", "name": "Hero", "url": "/api/v1/public/media/..." }
  ]
}
```

Solo se devuelven bloques habilitados, ordenados por `sort_order`. No se exponen usuarios, pagos, check-ins, fotos privadas de miembros ni `storage_key`.

## Edición

- `GET|PATCH /api/v1/site/settings`.
- CRUD `/api/v1/site/landing-blocks`.
- CRUD `/api/v1/site/media` y `POST /api/v1/site/media/upload`.
- `GET /api/v1/public/media/{id}`.

Pueden editar el sitio:

- Un `platformAdmin`.
- Un `owner` de al menos una sucursal activa.

Un `owner` no necesita una sucursal activa seleccionada para editar la landing; el contenido es global.

## Seguridad

- La consulta pública no requiere JWT y no depende de una sucursal.
- Las imágenes locales del sitio se entregan por `/api/v1/public/media/{id}`.
- Las fotos de miembros viven en `member_photos` y se sirven por un endpoint privado con Bearer.
- Los assets externos deben ser URLs HTTPS.

## Pruebas requeridas

1. `GET /api/v1/public/site` responde sin JWT.
2. Un bloque deshabilitado desaparece de la siguiente consulta.
3. Cambiar título, tema o imágenes actualiza la landing sin publicar.
4. Un `owner` puede editar el sitio; un `trainer` o `member` recibe 403.
5. La respuesta pública no incluye datos privados.
6. No existen rutas `/g/{slug}` ni endpoints `/public/gyms/{slug}/landing`.
