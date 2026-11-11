# GMS Backend — seguridad A–K aplicada a `src(1).zip`

**Fecha:** 10/11/2026. Versión base: archivo actual `src(1).zip` proporcionado por el usuario. Se preservaron los cambios previos de configuración local/WildFly, Liquibase, el esquema consolidado, seed y pruebas de kiosco.

## Resultado de integración

| Punto | Corrección | Estado / limitación |
|---|---|---|
| A | Protección de login en memoria Java: 5 contraseñas fallidas por cuenta en ventana móvil de 15 minutos; sexto intento devuelve `429` y `Retry-After`; umbral complementario por IP. | Aplicado. Cada JVM tiene sus propios contadores; reiniciar borra el historial. |
| B | JWT nuevos con claim interno `sid`; validación del estado de `user_sessions` en cada uso y revocación de acceso al cerrar sesión o rotar refresh. | Aplicado. JWT anteriores al despliegue sin `sid` conservan su vida restante (máximo 1 hora). |
| C | Validación de `iss=gym-back` y tiempos en `NimbusJwtDecoder`. | Aplicado. |
| D | `POST /api/v1/gyms/{gymId}/payments` permite `Idempotency-Key` opcional, deduplicación transaccional y recuperación del pago previo. | Aplicado. Sin header, los reintentos siguen pudiendo duplicar operaciones. |
| E | `POST /api/v1/gyms/{gymId}/sales` admite la misma protección sin doble descuento de inventario. | Aplicado. Misma clave/datos: mismo `201` y venta; misma clave/distintos datos: `409`. |
| F | Mantener permisos del entrenador. | Sin modificaciones, decisión explícita del usuario. |
| G | Validar zona horaria IANA al crear/editar gimnasio. | Aplicado. Las otras validaciones de la entidad permanecen intactas. |
| H | Reducir escrituras de `lastSeenAt` del kiosco a una frecuencia aproximada de 5 minutos. | Mitigación parcial; no se borra la auditoría. Política de retención pendiente. |
| I | Obtener IP de login y activación de kiosco considerando `X-Forwarded-For` **solo** si el peer es proxy de confianza. | Aplicado; configurar `TRUSTED_PROXY_IPS` si hay reverse proxy. |
| J | Conservar QR, código manual y los cuatro flujos de check-in. | Sin cambios de negocio; retransmisión remota de QR sigue siendo riesgo aceptado. |
| K | Pagos de planes informativos de $0 devuelven `400` controlado antes de intentar guardar un importe incompatible con la BD. | Aplicado; los planes gratuitos siguen visibles. |

## Diferencia importante frente al ZIP anterior

El ZIP actual trae `application.yml` con `spring.flyway.enabled: false`, un baseline de Liquibase en `db/schema/schema.sql`, un seed y el archivo `db/changelog-master.yaml`. **Esos archivos originales fueron conservados**. En particular, no se modificó el baseline ya aplicado ni el nuevo `V1__base_schema.sql`.

Para llevar la idempotencia al gestor de migraciones **de esta versión** se añadió:

1. `src/main/resources/db/schema/idempotency_operations.sql`: changeset Liquibase nuevo `gms:idempotency-operations-001` con tabla `request_idempotency`, índice, claves y triggers de auditoría.
2. Una referencia adicional a ese changeset **al final** de `db/changelog-master.yaml`.
3. `src/main/resources/db/migration/V7__money_operation_idempotency.sql` queda disponible únicamente para entornos que **sigan ejecutando Flyway** (por ejemplo, pruebas de integración configuradas expresamente así).

**Importante:** Liquibase y Flyway son rutas de migración alternativas. **Nunca ejecutar las dos contra la misma base de datos para crear esta tabla**. Confirma que el entorno de despliegue usa realmente `classpath:db/changelog-master.yaml`; esa referencia puede estar definida en un perfil o configuración de servidor no incluida en el ZIP.

El servicio `IdempotencyService` ahora califica la tabla usando `${schema_db}` (o `SCHEMA_DB`, default `test`), por lo que no depende de que `SET search_path` esté presente en el pool de conexiones de WildFly. No se alteró la configuración `application.yml` actual.

## Compatibilidad de frontend

No se cambian endpoints existentes, DTO de login, refresh, logout ni `GET /auth/me`; el éxito de pago y venta conserva status `201` y los mismos campos JSON. No se modificó ningún endpoint de check-in, ni las políticas de roles.

Para aprovechar D y E, el frontend debe generar **una clave UUID por operación** y reutilizar **esa misma clave** al reintentar la misma acción. Se envía como encabezado `Idempotency-Key`. Si el frontend no envía el encabezado, la llamada sigue funcionando como antes, pero no obtiene deduplicación.

## Configuración nueva opcional

`TRUSTED_PROXY_IPS=10.0.0.10,10.0.0.11` (solo ejemplo) identifica IPs exactas de proxies administrados por ti. Si no hay proxy de confianza, **dejar vacío**. No aceptar `X-Forwarded-For` cuando la conexión llega directamente de internet.

## Pruebas necesarias antes de producción

1. Compilar y ejecutar `mvn clean test` en el proyecto completo que contiene `pom.xml`.
2. Ejecutar Liquibase en staging, verificar que `request_idempotency` exista en el esquema correcto y que el changeset figure como ejecutado en `databasechangelog`.
3. Login: cinco contraseñas incorrectas con igual correo (aunque cambie IP); verificar `401` en las cinco y `429` en la siguiente; comprobar `Retry-After` y acceso de otras cuentas.
4. Login/refresh/logout: verificar `LoginResponse` idéntico y que el JWT nuevo deje de funcionar tras logout; comprobar `iss` incorrecto -> `401`.
5. Pagos: misma clave y mismo JSON dos veces -> **un pago** y **una sola extensión** de membresía; clave repetida con JSON distinto -> `409`.
6. Ventas: misma clave y mismo JSON dos veces -> **una venta** y **una sola reducción** de stock; respuesta sin cambios de campos.
7. Plan informativo de precio cero -> `400` descriptivo, sin insertar pago ni suscripción.
8. Prueba de todos los roles, QR de gimnasio/miembro, código manual, check-in administrativo y activación/cookies del kiosco.
9. Con proxy real, comprobar IP de límite de login y activación de kiosco sin confiar en headers enviados por clientes directos.
10. Monitorear latencia de BD: validar el `sid` agrega una consulta a `user_sessions` por petición JWT. Revisar crecimiento de auditoría.

## Comprobación de entrega

- El ZIP actual se usó como base y se conservan todos sus archivos.
- Solamente se transfirieron los 20 cambios Java que coincidían byte a byte con la versión anterior, se agregaron nuevas clases/pruebas y migraciones, y se añadió un `include` incremental en Liquibase.
- El `application.yml` actual, `V1__base_schema.sql`, baseline Liquibase, seed y `KioskPostgresIntegrationTest.java` originales quedaron sin tocar.
- Revisión sintáctica de **232 archivos Java**, sin errores de parseo.
- **No se certifica compilación, ejecución Maven, integración real PostgreSQL/Liquibase ni producción**: el ZIP proporcionado contiene `src/` sin `pom.xml` y no hay BD de pruebas disponible en este entorno.
