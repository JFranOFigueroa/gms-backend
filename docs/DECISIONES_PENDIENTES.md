> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

# Decisiones confirmadas, pendientes y exclusiones

## Confirmadas (2026-11-03)

- Cuenta global: alta **exclusivamente manual por SQL** por ahora, con `password_hash` BCrypt y `status='active'`.
- `POST /members` y `POST /staff` enlazan cuentas existentes por `userId`; no crean `invited`.
- Selector de gimnasio **dentro del flujo login**, mostrado después de credenciales válidas: `gyms[]` y `roles[]` por gimnasio. `/auth/me` comparte exactamente el modelo `user+gyms` de login. `gymId` de las operaciones tenant se valida en cada petición.
- Pagos son **manuales, administrativos y sin pasarela**. Errores de captura bajo responsabilidad de operador, sin anulación/reembolso automático en alcance actual.
- Primero se completa/prueba backend; luego se adaptará Vue. Orden de módulos a integrar: aún no fijado.
- La categoría `functional` se agrega con Flyway V2; en reserva regular aplica límite diario como `normal` provisionalmente.

## Sin decidir / NO implementar por ahora

1. Matriz definitiva de permisos por rol y operación. Los permisos codificados hoy son **provisionales**; no presentarlos como política aceptada.
2. Registro, invitación, activación y recuperación de contraseña desde la aplicación. Sin endpoints de alta de identidad global.
3. Cambio de roles cuando un usuario tiene simultáneamente owner/trainer en un gimnasio: el PATCH por `userId` rechaza ambigüedad, no desactiva roles arbitrariamente. Definir flujo cuando se aborde.
4. Comportamiento preciso de `functional` en reglas de calendario: actualmente cuenta como clase regular para evitar saltarse la restricción diaria. Confirmar cuando se establezcan políticas.
5. Políticas completas de reservas, invitaciones y acceso QR: kiosco, tokens, OTP, anti-replay y límites de invitados.
6. Integración de frontend Vue, contratos definitivos de cada store y orden por módulos.
7. Archivos cloud, workers, notificaciones y auditoría transversal append-only.
8. Fiscalidad/reembolsos/devoluciones/conciliación, pagos parciales y corrección de cobros erróneos: no forman parte del registro administrativo básico actual.

## Límites técnicos (no son decisiones de negocio)

- No se ha ejecutado `mvn test` ni levantado PostgreSQL en este entorno; falta validación real.
- V1 y V2 deben probarse en DB desechable con esquema configurado. Si V1 ya se aplicó en producción, NO editarla.
- Pruebas de aislamiento tenant, concurrencia checkout/reservas y permisos por rol deben ejecutarse con PostgreSQL.
- La tabla `user_sessions` existe en SQL, pero los JWT actuales duran una hora y no hay un flujo completo de refresh/revocación ni gestor de sesiones.
- No despliegue a producción hasta validar seguridad y migraciones.

## Propuesta implementada detrás de bandera (03/11/2026)

La nueva especificación plantea un cambio de alcance: CRUD de identidad y registro guiado de miembro/staff. Se incluye código detrás de `USER_API_ENABLED=false` para **no invalidar la decisión confirmada de alta manual por SQL** mientras no se apruebe la activación. Consultar `INTEGRACION_BACKEND_ESTADO.md`. Siguen sin decidirse emparejamiento QR, proveedor de media, matriz definitiva de permisos, guest-passes y activación de las altas globales.
