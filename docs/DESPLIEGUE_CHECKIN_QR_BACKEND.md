# Despliegue del módulo Check-in QR

## Arquitectura

```text
Controller -> Service -> I*Repository -> Repository JPA -> PostgreSQL
```

- La política de acceso es compartida entre check-in manual, QR y código temporal.
- Los controladores no contienen SQL.
- El dispositivo del kiosco se autentica con cookie HttpOnly.
- El kiosco se identifica por el código de sucursal, no por una landing.

## Base de datos

El esquema completo está en `src/main/resources/db/schema/schema.sql` y se aplica con Liquibase. No hay migraciones Flyway V5/V6 en el proyecto actual.

Tablas relevantes:

- `checkin_devices`
- `checkin_device_sessions`
- `checkin_activation_attempts`
- `checkin_qr_tokens`
- `checkin_events`
- `request_idempotency`

`v_member_access` devuelve `unknown_validity` si una suscripción pagada no tiene fecha de vencimiento: no autoriza por datos incompletos.

## Variables

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gymback
DB_USER=gymback
DB_PASSWORD=<secreto>
schema_db=test
JWT_SECRET=<secreto-aleatorio-de-32-bytes-o-mas>
CHECKIN_MEMBER_QR_SECRET=<secreto-aleatorio-distinto>
KIOSK_CSRF_SECRET=<otro-secreto-aleatorio>
FRONTEND_ORIGIN=https://app.tudominio.mx
KIOSK_COOKIE_SECURE=true
```

La cookie `GMS_KIOSK` es `HttpOnly`, `Secure`, `SameSite=Strict` y se limita al path `/api/v1/kiosk`. En local se puede usar `KIOSK_COOKIE_SECURE=false`.

## Orden de despliegue

1. Aplicar el baseline con Liquibase.
2. Ejecutar el seed idempotente.
3. Verificar `spring.jpa.hibernate.ddl-auto=validate`.
4. Configurar variables y HTTPS.
5. Arrancar la aplicación.
6. Activar un dispositivo de kiosco desde el dashboard.
7. Probar QR de sucursal, QR de miembro, código manual y check-in administrativo.
8. Verificar que un kiosco de otra sucursal no pueda operar sobre la sesión activa.

## Seguridad

- Código de activación: 256 bits, hash persistido, 10 minutos, un solo uso; límite de fallos por IP.
- Sesión de kiosco: cookie aleatoria con hash en BD, revocable por dispositivo; se revisa sucursal/módulo/dispositivo en cada llamada.
- QR de sucursal: hash persistido, TTL configurable entre 5 y 300 segundos; límite de emisiones por dispositivo.
- QR de miembro: credencial firmada de 30 segundos, propósito y sucursal ligados.
- Código manual: 12 caracteres Base32, TTL 120 s, 3 emisiones/minuto/miembro, una sola utilización.
- Cada check-in QR exige `Idempotency-Key`; reintentos idénticos no duplican eventos.
- El kiosco no lee nombres, correos, pagos ni listados privados.

## Pruebas

- `KioskPostgresIntegrationTest` cubre HTTP real, seguridad, activación, QR, código manual, idempotencia y auditoría.
- Ejecutar con un esquema preparado externamente:

```sh
GMS_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55432/gms_checkin_audit
GMS_TEST_DB_PASSWORD=...
mvn -Dtest=KioskPostgresIntegrationTest test
```

- Para producción, validar además CORS/cookies reales, concurrencia y un dispositivo físico.
