# Estado de integración backend — revisión sin publicación manual

**Decisión funcional vigente:** existe una página pública por gimnasio activo. La ruta compartible se basa en `gym.slug`; lo que se guarda en el editor se consulta directamente sin etapa publicar/despublicar. Esta revisión **sustituye** al flujo de snapshots de la propuesta histórica, conservada en `docs/INTEGRACION_BACKEND_ESTADO_HISTORICO_20261103.md`.

## Cambios efectivos sobre el backend corregido

- Se retiran del controlador **únicamente** las operaciones de publicación manual `POST /gyms/{gymId}/landing/publish` y `POST /gyms/{gymId}/landing/unpublish`.
- `GET /gyms/{gymId}/landing/link` (nuevo) y `GET .../landing/publication` (alias temporal compatible) devuelven `gymId`, `gymName`, `slug` y `publicUrl`; no contienen estado de publicación, revisión de borrador ni versión de snapshot.
- `PATCH /gyms/{gymId}/landing/slug` continúa existiendo, valida permisos owner/admin, normaliza el slug y controla colisiones (409). El slug sigue siendo único globalmente, pero cada dato sigue teniendo `gymId` propio.
- `GET /public/gyms/{slug}/landing` consulta el gimnasio activo y sus bloques habilitados actuales, ordenados, y responde `gym`, `theme`, `blocks`, `mediaAssets`. Es anónimo; no depende de `landing_published` ni de la tabla `landing_publications`.
- El repositorio público ofrece imágenes HTTPS únicamente si están referenciadas en las diapositivas de bloques públicos visibles. No expone los archivos privados, `storage_key`, usuarios, pagos ni datos personales.
- `SettingsService.patch()` rechaza el cambio a `landingPublished`; ese campo de V1 queda de legado sin efecto sobre la visibilidad del sitio.
- **Se mantiene `V3__landing_publications.sql` sin modificar** por compatibilidad de Flyway; no se vuelve a consultar/escribir su tabla. No ejecutar `DROP TABLE`, cambiar checksums de Flyway ni reconstruir la BD.
- Los tests `LandingPublicationServiceTest.java` ahora cubren enlace automático, modificación de slug, conflictos y consulta pública sin sesión; dejaron de probar operaciones retiradas.

## Resto de módulos conservados

Permanecen los controladores, repositorios y services de cuentas, miembros, personal, planes, pagos, ventas, rutinas, calendario, reservas propias, historial de check-ins, dashboard y media-assets. El CRUD de bloques, temas, ordenación, status del gym y generación de slug al crear un gimnasio no cambian de comportamiento.

Se conserva `USER_API_ENABLED=false` por defecto; para ejecutar altas nuevas de miembros, entrenadores y usuarios se requiere habilitarlo explícitamente y validar permisos. Los endpoints seguros QR/kiosco, almacenamiento de binarios y guest passes siguen pendientes.

## Validación pendiente antes de despliegue

El material entregado sigue siendo un directorio Java `src/` y documentación, **sin `pom.xml`**. Por ello se necesita integrar en el proyecto Maven original y ejecutar en tu equipo:

```bash
mvn clean test
mvn spring-boot:run
```

Verificar los siguientes flujos por HTTP: dueño A edita y cambia slug de A, no de B; visitante ve bloques guardados de A inmediatamente después de guardar; slug anterior 404; gimnasio suspendido 404; medios privados no aparecen. Validar Flyway sobre una base nueva y otra con V3 instalada, sin ejecutar migraciones a mano.

Consultar `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para el contrato vigente detallado.
