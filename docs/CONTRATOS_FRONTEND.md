> **Integración vigente:** ver [sesión, imágenes y comportamiento de pantallas](GUIA_FRONTEND_SESION_IMAGENES.md); conserva los contratos anteriores no mencionados.

> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

# Contratos de backend para futura integración con Vue

**Estado:** contrato del backend actualizado. **Vue NO se modificó**; el frontend se adaptará cuando el backend esté probado, y el orden de conexión de módulos queda por definir. No inventar endpoints.

## Login con selector de gimnasios

El selector ocurre **dentro del flujo de login, después de validar credenciales** para no exponer por correo en un endpoint público a qué gimnasios pertenece una persona. La interfaz puede mantener al usuario en la pantalla de login hasta seleccionar gimnasio; luego entra en la aplicación con su JWT y gymId activo. La elección de gimnasio es **estado del frontend**; no se agrega un endpoint especial para elegirlo. Cada petición `/gyms/{gymId}/...` requiere autorización del servidor.

**POST `/api/v1/auth/login`**

Solicitud:
```json
{"email":"ana@ejemplo.com","password":"contraseña del usuario"}
```

Respuesta de ejemplo (UUID ficticios ilustrativos):
```json
{
  "token":"JWT_FIRMADO",
  "tokenType":"Bearer",
  "user":{
    "id":"00000000-0000-0000-0000-000000000001",
    "name":"Ana",
    "email":"ana@ejemplo.com",
    "platformAdmin":false,
    "defaultGymId":"00000000-0000-0000-0000-000000000010"
  },
  "gyms":[
    {"id":"00000000-0000-0000-0000-000000000010","name":"Gym A","slug":"gym-a","roles":["owner","trainer"]},
    {"id":"00000000-0000-0000-0000-000000000020","name":"Gym B","slug":"gym-b","roles":["member"]}
  ]
}
```

**GET `/api/v1/auth/me`** con `Authorization: Bearer <token>`: devuelve **los mismos campos `user` y `gyms`**, sin token, para restaurar sesión/selector; `user.role` **NO EXISTE** porque la persona puede tener roles distintos en diferentes gimnasios. `defaultGymId` es `null` si ya no existe acceso. Un admin global tiene `platformAdmin=true` y ve todos los gimnasios activos, con rol `admin` y roles locales si los posee. No se agregaron roles falsos para usuarios ordinarios sin asignación.

## Cuentas globales y asociaciones

Los usuarios globales se insertan **manualmente por SQL** en `${schema_db}.users`, con `status='active'`, email válido y hash BCrypt de contraseña; la API **no crea** cuentas ni invitaciones.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/v1/gyms/{gymId}/members` | Lista expedientes del gym |
| GET | `/api/v1/gyms/{gymId}/members/{id}` | Expediente por ID |
| POST | `/api/v1/gyms/{gymId}/members` | Asociar usuario existente activo |
| PATCH | `/api/v1/gyms/{gymId}/members/{id}` | Editar SOLO expediente, nunca identidad |
| DELETE | `/api/v1/gyms/{gymId}/members/{id}` | Inactivar expediente |
| GET | `/api/v1/gyms/{gymId}/staff` | Lista agregada por usuario con `roles[]` |
| GET | `/api/v1/gyms/{gymId}/staff/{userId}` | Persona y sus roles |
| POST | `/api/v1/gyms/{gymId}/staff` | Asociar usuario existente activo con un rol |
| PATCH | `/api/v1/gyms/{gymId}/staff/{userId}` | Cambiar rol único existente, con protección de ambigüedad |
| DELETE | `/api/v1/gyms/{gymId}/staff/{userId}` | Desactivar asignaciones autorizadas |

Alta de expediente:
```json
{"userId":"00000000-0000-0000-0000-000000000001","code":"M-001","active":true}
```

PATCH de expediente (NO se acepta userId):
```json
{"code":"M-001A","emergencyContactName":"Contacto","active":true}
```

Alta staff:
```json
{"userId":"00000000-0000-0000-0000-000000000001","role":"trainer"}
```

PATCH staff:
```json
{"role":"trainer"}
```

Cuando una persona tiene simultáneamente `owner` y `trainer`, el GET devuelve ambos en `roles[]`. Su PATCH por userId se rechaza en lugar de reemplazar arbitrariamente alguno: el flujo de cambio multirrol sigue sin definirse. Los permisos preexistentes permanecen **provisionales** hasta acordar matriz por módulo.

## Módulos y endpoints existentes (NO creados ahora)

| Área | Rutas existentes | Estado / contrato |
|---|---|---|
| Gym | `GET,POST /api/v1/gyms`; `GET,PATCH,DELETE /api/v1/gyms/{id}` | admin global y políticas previas |
| Planes | `GET,POST /gyms/{g}/plans`; `GET,PATCH,DELETE /{id}` | CRUD planes |
| Pagos | `GET,POST /gyms/{g}/payments` | **Registro manual**, no pasarela ni anulación automática |
| Plantillas de clase | `GET,POST /gyms/{g}/class-templates`; `GET,PATCH,DELETE /{id}` | `category` admite functional tras V2 |
| Clases | `GET,POST /gyms/{g}/classes`; `GET,PATCH,DELETE /{id}` | `normal,functional,special,overlay_special,event` |
| Reservas | `GET,POST /gyms/{g}/reservations`; `DELETE /{id}` | Regla provisional: functional cuenta como regular |
| Rutinas | `GET,POST /gyms/{g}/routines`; `GET,PATCH,DELETE /{id}` | miembros solo pueden leer published=true |
| Productos | `GET,POST /gyms/{g}/products`; `GET,PATCH,DELETE /{id}` | stock no modificable desde totales del cliente |
| Ventas | `GET,POST /gyms/{g}/sales` | Respuesta con `items[]` en lectura y checkout |
| Checkins | `GET,POST /gyms/{g}/checkins` | manual; QR público pendiente |
| Configuración | `GET,PATCH /gyms/{g}/settings` | configuración tenant |
| Módulos | `GET /gyms/{g}/modules`, `PATCH /gyms/{g}/modules/{key}` | módulos habilitados |
| Landing | `GET,POST /gyms/{g}/landing-blocks`; `GET,PATCH,DELETE /{id}` | solo builder autenticado |
| Media | `GET,POST /gyms/{g}/media-assets`; `GET,PATCH,DELETE /{id}` | metadatos HTTPS, no subida cloud |

Todos los endpoints `/gyms/{g}/...` tienen prefijo **`/api/v1`**; se omitió en algunas celdas por brevedad.

### Venta con ítems — GET y respuesta de POST

```json
{
 "id":"00000000-0000-0000-0000-000000000100",
 "gymId":"00000000-0000-0000-0000-000000000010",
 "total":80.00,
 "currency":"MXN",
 "status":"completed",
 "soldBy":"00000000-0000-0000-0000-000000000001",
 "soldAt":"2026-11-03T17:00:00Z",
 "items":[{
   "id":"00000000-0000-0000-0000-000000000101",
   "productId":"00000000-0000-0000-0000-000000000102",
   "productName":"Agua","quantity":2,"unitPrice":40.00,"lineTotal":80.00
 }]
}
```

La estructura es `SaleView`; `GET /sales` retorna una lista de este tipo. Los precios y totales se calculan con DB; `POST /sales` solo recibe `{"items":[{"productId":"UUID","quantity":2}]}`.

## Adaptaciones pendientes del frontend (NO incluidas en este ZIP)

- Login con selección **post-autenticación** antes de dashboard; al restaurar sesión, usar `/auth/me`.
- Dejar de usar `user.role` global; determinar capacidades según `roles[]` del gimnasio seleccionado. Autorización definitiva siempre del backend.
- IDs UUID en lugar de numéricos; mapear camelCase vs nombres del store.
- Las altas de miembro/staff envían `userId` de una cuenta precargada; **no envían name/email/password**.
- Ventas leen `items[]` del servidor; calendario admite `functional`; rutinas y miembros no deben asumir borradores visibles.
- Media/QR/notificaciones/jobs: flujos pendientes, no asumir disponibles.

## Nuevos contratos implementados parcialmente (03/11/2026)

El mapa completo, códigos, prioridades y banderas de las 28 propuestas y las 18 rutas implementadas están en `INTEGRACION_BACKEND_ESTADO.md`. Vue deberá añadir la ruta pública `/g/:slug` que consume `GET /api/v1/public/gyms/{slug}/landing` **sin JWT**, y configurar fallback SPA; esta entrega no edita Vue. Las altas guiadas de personas requieren `USER_API_ENABLED=true`. `GET /classes` ahora filtra a `scheduled` por defecto y acepta `status=all` para mostrar canceladas/completadas. Nunca tratar planes del bloque decorativo como compras o suscripciones.
