# Guía de integración frontend — sesión, imágenes y comportamiento de pantallas

**API base:** `/api/v1` · **Destinatario:** frontend Vue/Pinia · **Versión:** backend JPA refactorizado, 03/11/2026.

> Esta guía describe únicamente los contratos y comportamientos que el frontend debe consumir. Conservar las pantallas, textos y campos de negocio originales. No mostrar identificadores UUID, claves de almacenamiento, rutas del servidor ni datos técnicos en la interfaz: los identificadores se utilizan exclusivamente en las peticiones internas.

## 1. Funcionalidades que se conservan

- Permanecen las rutas REST de gimnasios, miembros, staff, planes, clases, reservas, rutinas, punto de venta, pagos, check-in, configuración, dashboard y landing.
- La página de cada gimnasio se consulta por su **slug**: `GET /api/v1/public/gyms/{slug}/landing`. No crear botones ni rutas de publicar/despublicar.
- Conservar `GET /api/v1/gyms/{gymId}/landing/link` y `PATCH /api/v1/gyms/{gymId}/landing/slug`.
- Mantener **un solo campo de correo** en los formularios que ya lo tenían y no alterar los textos originales de check-in u otras pantallas.
- `Authorization: Bearer <access_token>` continúa siendo necesario para endpoints privados. Se conservan los permisos de cada rol y gimnasio.

## 2. Inicio de sesión y renovación del acceso

### `POST /api/v1/auth/login` (sin Bearer)

**Request existente, sin cambios:**

```json
{"email":"persona@ejemplo.com","password":"clave-del-usuario"}
```

**200:** se conservan los campos `token`, `tokenType`, `user` y `gyms`; ahora se reciben también `refreshToken` y `expiresIn`.

```json
{
  "token": "<jwt-acceso>",
  "tokenType": "Bearer",
  "user": {"id":"<uuid>","name":"...","email":"...","platformAdmin":false,"defaultGymId":"<uuid>"},
  "gyms": [{"id":"<uuid>","name":"...","slug":"...","roles":["owner"]}],
  "refreshToken": "<secreto-renovable>",
  "expiresIn": 3600
}
```

`expiresIn` es la duración del access token en segundos. El `refreshToken` es distinto del JWT de acceso; no utilizarlo como Bearer para otras rutas ni registrarlo en logs o analytics.

### `POST /api/v1/auth/refresh` (sin Bearer)

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

```json
{"refreshToken":"<refresh-vigente>"}
```

**200:** misma estructura de respuesta que login, con un `token` y un `refreshToken` nuevos. **Reemplazar ambos valores** y descartar inmediatamente el refresh anterior.

**401:** el refresh ya no es válido; cerrar la sesión local y mostrar la pantalla de acceso. No repetir indefinidamente la solicitud.

### `POST /api/v1/auth/logout` (sin Bearer)

```json
{"refreshToken":"<refresh-vigente>"}
```

**204:** limpiar tokens, datos del usuario, gimnasio seleccionado y cachés locales de información privada.

### Integración con Axios/Pinia

```js
// Ejemplo de referencia: adaptar a la store y al cliente HTTP existentes.
let renewal = null;

async function renewSession(api, refreshToken) {
  if (!renewal) {
    renewal = api.post('/auth/refresh', { refreshToken })
      .then(({ data }) => {
        sessionStore.setTokens(data.token, data.refreshToken, data.expiresIn);
        sessionStore.setIdentity(data.user, data.gyms);
        return data.token;
      })
      .finally(() => { renewal = null; });
  }
  return renewal;
}

// Ante 401 en endpoint privado: renovar una vez y reintentar una vez.
// Excluir /auth/login, /auth/refresh y /auth/logout para evitar bucles.
// Si falla: limpiar sesión y redirigir al login.
```

**Seguridad:** no conservar refresh tokens en `localStorage` como solución definitiva. Priorizar almacenamiento en memoria o definir una solución con cookie `HttpOnly`/`Secure`/`SameSite` en una evolución coordinada con backend; el contrato actual envía los tokens en JSON, no mediante cookies.

## 3. Subida y visualización de imágenes

### `POST /api/v1/gyms/{gymId}/media-assets/upload`

**Permiso:** owner del gimnasio o administrador global. Utilizar `multipart/form-data`.

| Campo | Tipo | Descripción |
|---|---|---|
| `file` | File | Obligatorio. PNG, JPEG o WebP; máximo 8 MiB. |
| `name` | String | Opcional. Nombre descriptivo. |

```js
const body = new FormData();
body.append('file', imageFile);
body.append('name', 'Portada del gimnasio');

const { data: asset } = await api.post(`/gyms/${gymId}/media-assets/upload`, body);
const assetId = asset.id; // Solo para referencias internas del frontend.
// Axios define el multipart boundary; no establecerlo manualmente.
```

**201:** devuelve el recurso creado, incluyendo `id`, `gymId`, `name`, `mediaType`, `mimeType`, `byteSize` y `contentUrl`. Solo mostrar en pantalla los datos útiles para la persona (por ejemplo, imagen y nombre).

Se mantiene `POST /api/v1/gyms/{gymId}/media-assets` para asociar imágenes mediante URL HTTPS externa. Para reemplazar un archivo local, subir otra imagen y actualizar la referencia correspondiente.

### Imágenes privadas: foto de perfil o miembro

`GET /api/v1/gyms/{gymId}/media-assets/{assetId}/content` necesita el Bearer de un usuario autorizado. Un `<img>` estándar no añade el token; solicitar el recurso como `blob`:

```js
const response = await api.get(`/gyms/${gymId}/media-assets/${assetId}/content`, {
  responseType: 'blob'
});
const temporaryUrl = URL.createObjectURL(response.data);
// Usar temporaryUrl como src y liberar con URL.revokeObjectURL(temporaryUrl)
// al cambiar la foto o desmontar el componente.
```

Para asociar una foto a un miembro, mantener el endpoint existente:

```http
PATCH /api/v1/gyms/{gymId}/members/{memberId}
Authorization: Bearer ...
Content-Type: application/json
```

```json
{"photoAssetId":"<uuid-de-la-imagen>"}
```

El detalle de miembro conserva `photoUrl`. Si la URL corresponde a contenido privado, recuperarla con el cliente autenticado en lugar de asignarla directamente a `<img src>`.

### Imágenes públicas: landing

Las imágenes que el backend devuelve como públicas pueden cargarse sin Bearer desde `GET /api/v1/public/media/{assetId}`.

El editor puede guardar referencias de imágenes en el JSON de los bloques que ya existían. Ejemplo de propiedades compatibles (solo utilizar las que correspondan a cada bloque):

```json
{"imageAssetId":"<uuid>","coverAssetId":"<uuid>","backgroundAssetId":"<uuid>","slides":["<uuid>"]}
```

`GET /api/v1/public/gyms/{slug}/landing` mantiene `gym`, `theme`, `blocks` y `mediaAssets`. Para imágenes locales, `mediaAssets[].url` apunta a `/api/v1/public/media/{id}`. Resolver las URLs relativas contra el origen de la **API**:

```js
const imageSrc = new URL(media.url, API_ORIGIN).toString();
```

Para cambiar el logo desde el perfil del propietario:

```http
PATCH /api/v1/gyms/{gymId}/landing/logo
Authorization: Bearer <token-owner>
Content-Type: application/json
```

```json
{"imageAssetId":"<uuid-de-imagen-local-del-mismo-gimnasio>"}
```

**200:**

```json
{"logoUrl":"/api/v1/public/media/<uuid>"}
```

La landing permanece accesible por el slug de cada gimnasio activo, sin un flujo de publicación manual.

## 4. Comportamiento al eliminar elementos

No se necesitan nuevos formularios, botones ni campos en el frontend para eliminar recursos. Conservar los `DELETE` ya existentes y sus confirmaciones originales.

- Al recibir **204 No Content**, quitar el elemento de la lista y actualizar los datos visibles.
- Si se solicita nuevamente un elemento que ya no está disponible, atender **404 Not Found** sin mostrar un error técnico.
- Una **reserva cancelada** sigue el comportamiento de cancelación establecido por su módulo; no reemplazarlo por un flujo de eliminación genérico.
- No crear opciones de **restauración** o **elementos eliminados**: no forman parte de las funcionalidades expuestas para estas pantallas.
- En vistas de clases, productos, miembros y planes, conservar las reglas de estado y disponibilidad que ya utiliza el frontend.

## 5. Manejo de errores

| Caso | HTTP esperado | Tratamiento en interfaz |
|---|---|---|
| Inicio de sesión o renovación inválidos | 401 | Finalizar sesión y mostrar el acceso. |
| Imagen privada sin autorización | 401 | Intentar una renovación y reintentar una vez. |
| Imagen inaccesible o inexistente | 404 | Mostrar una imagen de respaldo. |
| Usuario sin permiso para subir | 403 | Ocultar/deshabilitar la acción según rol. |
| Archivo inválido o mayor de 8 MiB | 400 o 413 | Mostrar mensaje sencillo de validación. |
| Slug o valor único ya ocupado | 409 | Explicar el conflicto con lenguaje claro. |
| Recurso no disponible | 404 | Actualizar la lista y evitar pantallas rotas. |
| Eliminación correcta | 204 | Actualizar la lista y mostrar confirmación. |

## 6. Zonas del frontend a revisar

1. **Autenticación:** cliente HTTP, store Pinia, login, cierre de sesión e interceptor para renovación automática una sola vez.
2. **Imágenes:** servicio multimedia, formularios de perfil, foto de miembro, logo, portada y componentes visuales; usar `FormData` y cargar imágenes privadas autenticadas.
3. **Landing:** seguir resolviendo la URL por slug y renderizar imágenes públicas a partir de `mediaAssets[].url`.
4. **Listados y eliminación:** conservar los flujos actuales; después del 204 actualizar la store y gestionar 404 de forma amigable.
5. **Experiencia de usuario:** no añadir campos, pantallas, textos o controles que no correspondan a las funciones del producto; preservar los textos originales, incluido check-in y el correo único de creación de usuarios.

## 7. Lista de comprobación frontend

- [ ] Inicio de sesión mantiene el selector actual de gimnasios y roles.
- [ ] Si vence el token, se renueva una vez y se reintenta la solicitud original.
- [ ] Si la renovación falla, se limpia sesión y se regresa al acceso, sin bucle.
- [ ] Logout limpia la sesión y la información privada local.
- [ ] Se pueden subir imágenes PNG/JPEG/WebP de hasta 8 MiB, con errores claros para archivos inválidos.
- [ ] Las imágenes privadas se recuperan con autorización y las URLs temporales se liberan.
- [ ] La landing muestra sus imágenes públicas sin token y el logo se puede actualizar por el propietario.
- [ ] Al eliminar un recurso, el elemento desaparece de las listas después de 204 y se gestiona 404.
- [ ] No hay botones de publicar/despublicar; el enlace personalizado por slug continúa funcionando.
- [ ] Las pantallas conservan sus textos, formularios y comportamientos originales, sin mostrar identificadores técnicos.

**Alcance:** documento exclusivamente de integración frontend. El código frontend no está incluido en esta entrega; aplicar los ajustes a sus servicios, stores y componentes existentes sin reemplazar pantallas completas.
