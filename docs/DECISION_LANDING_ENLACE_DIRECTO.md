# Decisión vigente: página por gimnasio y enlace personalizado

## Alcance aprobado

- Cada gimnasio activo tiene una página visible en `/g/{slug}` sin JWT; usa los **bloques y tema guardados en su propio gimnasio**, no una copia editorial ni un estado de publicación.
- El propietario edita bloques, orden, tema y medios como antes. Al guardar cambios en Landing Builder, el contenido público se actualiza en próximas consultas, sin pulsar «Publicar».
- Se conserva `gym.slug` único y normalizado para compartir un enlace legible; se puede personalizar sin alterar el nombre ni otros gimnasios. Cambiarlo invalida el enlace anterior.
- `GET /api/v1/gyms/{gymId}/landing/link` (autenticado, owner/admin) devuelve `{gymId,gymName,slug,publicUrl}`; `GET .../landing/publication` permanece como alias legado compatible, **sin devolver estado publicado**.
- `PATCH /api/v1/gyms/{gymId}/landing/slug` (autenticado) actualiza exclusivamente el slug.
- `GET /api/v1/public/gyms/{slug}/landing` (sin JWT) devuelve `{gym,theme,blocks,mediaAssets}` solo cuando `gym.status='active'`; los bloques son únicamente los `enabled` de ese gimnasio y se ordenan por `sort_order`.
- Los medios públicos se reducen a **imágenes con URL HTTPS** expresamente citadas en slides visibles de esos bloques, sin exponer `storage_key`, fotos privadas de miembros, pagos, usuarios o registros de acceso.
- Los endpoints `POST /landing/publish` y `POST /landing/unpublish` se retiran del controlador. `gym_settings.landing_published` deja de controlar la lectura pública; un PATCH de configuración que intente cambiarlo se rechaza expresamente.
- El esquema original V1 se mantiene sin cambios. **No borrar V3** (`landing_publications`), pues Flyway puede haberla aplicado en otros entornos. La tabla queda como legado sin utilizar: no se lee ni escribe para servir páginas; tampoco se precisa una migración adicional.

## Arquitectura y seguridad

La consulta pública se implementa en `PublicLandingController -> LandingPublicationService -> ILandingPublicationRepository -> LandingPublicationRepository`. El servicio no depende de la sesión del visitante; el repo resuelve solo un gimnasio activo por slug y obtiene sus bloques habilitados. Los controles editoriales siguen exigiendo autorización por gimnasio y el dueño de Gym A no puede modificar el enlace del Gym B. Nunca se confunde URL de Vue `/g/:slug` con el endpoint JSON `/api/v1/public/gyms/{slug}/landing`.

La visibilidad de una página se controla ahora por `gym.status` (activo vs suspendido/archivado), no por un botón de publicación. Si se quiere ocultar temporalmente solo la página sin suspender operaciones del gimnasio, se necesitará una decisión de producto y un endpoint separado; **no** está implementado.

## Integración de interfaz

- Mantener todos los componentes del editor y su autoguardado; reutilizar `LandingPublicationCard.vue` como tarjeta de «Página de tu gimnasio», sin controles publicar/despublicar.
- Formularios de altas muestran datos personales por defecto; los identificadores internos siguen en los contratos HTTP, no en pantalla. La opción secundaria de vincular una cuenta existente busca por correo con la API autorizada.
- Recuperar los textos visibles originales de Auditoría de Check-in y usar mensajes comprensibles sin términos técnicos del servidor.
- Las altas de cuentas siguen dependiendo de `USER_API_ENABLED` en el servidor, sin exponer ese nombre de configuración a usuarios.

## Pruebas requeridas

1. Gym A y Gym B con bloques distintos: `GET /public/gyms/a/landing` devuelve solamente A; B solamente B.
2. Eliminar o deshabilitar un bloque lo retira de la siguiente consulta pública; cambiar título/tema guardados lo actualiza sin publicar.
3. Gimnasio suspendido y slug inexistente devuelven 404 anónimo.
4. UUID de un gimnasio ajeno no permite cambiar el slug ni ver su editor.
5. El cambio de slug devuelve enlace nuevo y 404 con el anterior; slug repetido devuelve 409.
6. La API pública no incluye `users`, check-ins, pagos, `storage_key` ni imágenes privadas.
7. Sin borrar migraciones existentes: `mvn clean test`, Flyway en una base nueva y una con V3 instalada, y comprobaciones de Vue en `/g/:slug` en incógnito.
