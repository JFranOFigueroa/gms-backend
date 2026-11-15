# Verificación

## Comprobado en este entorno

- `mvn test`: 75 tests, 0 fallos, 17 omitidos.
- `KioskPostgresIntegrationTest` con PostgreSQL 15: 21 tests, 0 fallos, 1 omitido.
- `schema.sql`, `seed.sql` e `idempotency_operations.sql` aplicados en una base limpia.
- Seed reejecutado sin duplicados.
- Arranque local con `spring.jpa.hibernate.ddl-auto=validate`.
- `AdminBootstrap` creó el administrador y lo vinculó como `owner` de “Sucursal Demo”.
- Verificación HTTP:
  - `GET /api/v1/health`.
  - `POST /api/v1/auth/login`.
  - `GET /api/v1/auth/me`.
  - `GET /api/v1/public/site`.
  - `GET|PATCH /api/v1/site/settings`.
  - CRUD `/api/v1/site/landing-blocks`.
  - `POST /api/v1/site/media/upload` y `GET /api/v1/public/media/{id}`.
  - `GET /api/v1/branches`.
  - `GET /api/v1/branches/{id}/settings`.
  - `GET /api/v1/branches/{id}/members`.
  - `POST /api/v1/branches/{id}/members/{memberId}/photo` y lectura privada con Bearer.
- La lectura anónima de la foto de miembro devuelve 401.

## Pendiente

1. QA con navegador real: login admin sin sucursal, alta de sucursal, landing, multimedia, preview y landing pública.
2. Validación del despliegue en WildFly 40.
3. Pruebas E2E del kiosco con un dispositivo real.
4. Matriz definitiva de permisos.
5. Pruebas de concurrencia de ventas, reservas y check-in en PostgreSQL.
6. Revisión de accesibilidad y contraste de temas.

## Comandos

```bash
mvn clean test
mvn -Plocal spring-boot:run
curl http://localhost:8080/api/v1/health
```

Para el test de integración, preparar el esquema con Liquibase y definir:

```sh
GMS_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55432/gms_checkin_audit
GMS_TEST_DB_PASSWORD=...
```
