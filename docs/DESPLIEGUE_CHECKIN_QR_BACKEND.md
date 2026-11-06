# GMS · Instalación, seguridad y validación del módulo Check-in QR

## Cambios de arquitectura

Se mantiene `Controller → Service → I*Repository → Repository JPA → PostgreSQL`. Los repositorios de dispositivo, sesión y QR reutilizan `BaseCRUD` donde corresponde; las consultas y bloqueos especializados usan JPQL/EntityManager. El servicio comparte una política de acceso para manual y QR. No hay SQL directo en estos controladores/servicios. Los demás módulos no se modificaron.

## Base de datos

Agregar y ejecutar **V5__checkin_device_security_and_idempotency.sql**, sin editar V1–V4. Usa `${schema_db}` (`test` de forma predeterminada). Reutiliza las tablas `checkin_devices`, `checkin_qr_tokens`, `checkin_events`. Añade metadatos de activación, índice único de idempotencia, `checkin_device_sessions` y `checkin_activation_attempts`, con seis campos internos de control de cambios y protección contra eliminación física.

La vista `v_member_access` ahora devuelve `unknown_validity` si una suscripción pagada no tiene fecha de vencimiento: **no autoriza por datos incompletos**. Revisar primero los datos históricos con `next_due_date IS NULL`, ya que las entradas que antes se admitían por ausencia de fecha pasarán a rechazarse hasta que se regularicen.

## Variables de despliegue

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gymback
DB_USER=gymback
DB_PASSWORD=<secreto>
SCHEMA_DB=test
JWT_SECRET=<secreto-aleatorio-de-32-bytes-o-mas>
CHECKIN_MEMBER_QR_SECRET=<secreto-aleatorio-distinto-de-32-bytes-o-mas>
KIOSK_CSRF_SECRET=<otro-secreto-aleatorio-de-32-bytes-o-mas>
FRONTEND_ORIGIN=https://app.tudominio.mx
KIOSK_COOKIE_SECURE=true
```

La configuración conserva un **fallback de claves derivadas por propósito de JWT_SECRET** para no impedir que arranque el backend existente sin las dos nuevas variables, pero se recomienda usar los secretos independientes indicados arriba. Si se rotan secretos, los QR y CSRF de kioscos emitidos con la clave anterior dejan de ser válidos. **Nunca subir secretos a Git.**

El servidor debe usar HTTPS de extremo a extremo. La cookie `GMS_KIOSK` es `HttpOnly`, `Secure`, `SameSite=Strict` y solo se envía al path `/api/v1/kiosk`. Si frontend y API no comparten *site*, no habilitar cookies cross-site indiscriminadamente: diseñar y probar CORS/CSRF por separado. Para pruebas locales en `http://localhost`, se puede establecer `KIOSK_COOKIE_SECURE=false` **solo en desarrollo**.

`FRONTEND_ORIGIN` debe ser el origen **exacto** de Vue. Para kiosco, `GET /kiosk/session` entrega CSRF al origen autorizado; el frontend adjunta `X-CSRF-Token` y `withCredentials`. El resto de endpoints privados conserva JWT Bearer.

## Orden de despliegue

1. Restaurar este `src` y los documentos a la raíz del proyecto Maven **que contiene tu `pom.xml` real**; el ZIP recibido no lo incluía.
2. Respaldar la base de datos de pruebas y revisar membresías pagadas con fecha de vencimiento nula.
3. Comprobar `SCHEMA_DB=test`, variables anteriores y restricciones HTTPS/CORS.
4. Ejecutar `mvn clean verify` y después iniciar la API para que Flyway ejecute V5.
5. Validar que `spring.jpa.hibernate.ddl-auto=validate` compruebe las entidades nuevas y que Flyway no altere V1–V4.
6. Probar permisos/tenant para admin, owner, trainer y member; activar dispositivo, emitir QR, registrar por ambos métodos, repetir clave y probar expiración/revocación.
7. Comprobar los endpoints antiguos y regresiones de pagos, landing, usuarios, imágenes y refresh token.

**Nota sobre `target/`:** son binarios generados de una versión anterior del código. No se distribuyen en el ZIP nuevo para evitar ejecutar clases obsoletas. Generarlos de nuevo con Maven.

## Seguridad y límites operativos

- Código de activación: 256 bits, hash persistido, 10 minutos, un solo uso; 5 fallos por IP durante 15 minutos; para entornos detrás de proxy, configurar IP remota confiable sin aceptar `X-Forwarded-For` arbitrario.
- Sesión: cookie aleatoria con hash en BD, 12 horas, revocable por dispositivo, revisión de estado de gym/módulo/dispositivo en cada llamada.
- QR gimnasio: hash persistido, TTL de `gym_settings.checkin_qr_ttl_seconds` entre 5 y 300 segundos; 20 emisiones/minuto por dispositivo.
- QR miembro: credencial firmada de 30 segundos, propósito y gimnasio ligados; no es el JWT de login ni un QR estático.
- Validaciones: máximo 30 eventos/minuto por miembro y 120 eventos/minuto por dispositivo; controladores remiten `429` con `Retry-After`.
- Cada entrada QR requiere `Idempotency-Key` UUID; la clave se almacena hasheada por actor/tenant y compara fingerprint. Reintentos idénticos no crean evento adicional. Se mantiene el índice único diario por miembro.
- El dispositivo no lee nombres, correos, pagos ni listados privados. Los contadores de Check-in incluyen solo datos reales, no personas presentes.
- La tabla de QR y los intentos de activación pueden crecer: **definir y supervisar una política de retención/archivo lógico**, manteniendo la trazabilidad y los índices eficientes. No ejecutar `DELETE` físico manual.
- En producción complementar con límites en gateway/WAF y métricas de tiempos, rechazos, expiraciones y volumen; los límites de negocio en BD no reemplazan una defensa contra DDoS.

## Pruebas pendientes obligatorias

Las pruebas Java añadidas al ZIP cubren la firma, caducidad, CSRF, política de acceso y compatibilidad de contratos. Para aprobar producción deben ejecutarse **con el POM y PostgreSQL reales** las migraciones, pruebas JUnit/Mockito, endpoints HTTP, bloqueo de concurrencia y el navegador con CORS/cookies reales. El procesamiento de dos accesos paralelos debe producir como máximo un evento concedido por miembro/gym/fecha; validar que el otro es rechazo de duplicado. No se afirma que estos escenarios ya hayan pasado en un servidor real.

## Complemento V6: código manual temporal

Aplicar `V6__checkin_one_time_manual_codes.sql` **después** de V5. Reutiliza `${schema_db}` y triggers V4; no hace falta cambiar secretos, JWT, cookies ni configuración de QR. El código manual son 12 caracteres Base32 en tres bloques, TTL 120 s, 3 emisiones/minuto/miembro, 10 validaciones/minuto/dispositivo, una sola utilización. Los hashes de códigos e históricos de rechazo se conservan conforme a la política de borrado lógico. Ver `docs/GUIA_FRONTEND_CHECKIN_CODIGO_Y_SESION.md` para el frontend y `CAMBIOS_IMPLEMENTADOS_CHECKIN_V6.md` para archivos y pruebas. **No se ha ejecutado esta migración en PostgreSQL aquí.**
