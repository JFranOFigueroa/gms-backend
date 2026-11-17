# Gym Back — documento maestro del backend

**Main local:** `src/main/java/com/gymback/GymBackApplication.java`.
**Entrada WildFly:** `src/main/java/com/gymback/ServletInitializer.java`.
**Stack:** Java 21, Spring Boot 4.0.9, Spring Framework 7, Spring Security 7, Hibernate 7, Jackson 3, PostgreSQL 15+, Liquibase externo.

## 1. Regla de continuidad

Trabaja exclusivamente sobre este backend. Mantén:

```text
Controller -> Service -> I*Repository -> Repository -> PostgreSQL
```

- Las reglas de negocio viven en `Service`.
- Los controllers no acceden a JPA/JdbcTemplate.
- Los repositorios no contienen reglas de autorización.
- Las transacciones se declaran en los casos de uso.
- Toda consulta operativa se filtra por `branch_id` y valida permisos en servidor.
- No agregues endpoints, tablas o flujos sin requerimiento.
- Actualiza pruebas, contrato y documentación junto con el código.

## 2. Modelo sitio / sucursal

### Sitio (global)

La marca es única por instalación. No tiene `branch_id`.

- `site_settings`: identidad, contacto, moneda, timezone, tema.
- `site_landing_blocks`: bloques de la landing pública única.
- `site_media_assets`: imágenes/recurso del sitio.

Endpoints:

- `GET /api/v1/public/site` (anónimo).
- `GET|PATCH /api/v1/site/settings`.
- CRUD `/api/v1/site/landing-blocks`.
- CRUD `/api/v1/site/media` + `POST /site/media/upload`.
- `GET /api/v1/public/media/{id}` (anónimo).

Permisos de edición del sitio: `platformAdmin` u `owner` de al menos una sucursal activa.

### Sucursal (operativa)

- `branch`, `branch_settings`, `branch_modules`.
- `branch_members`, `branch_staff_roles`, `branch_classes`.
- `member_photos` (privada, separada de `site_media_assets`).

Todos los recursos operativos usan:

```text
/api/v1/branches/{branchId}/...
```

El `branchId` se valida contra la sesión y los roles del usuario. Un `platformAdmin` puede operar cualquier sucursal activa.

### Kiosco

El kiosco no usa una landing por sucursal:

- `GET /api/v1/public/branches/{code}/checkin-config`.
- `POST /api/v1/kiosk/activate` con `{ code, activationCode }`.
- Cookie HttpOnly de dispositivo.
- `GET /api/v1/kiosk/session`, `POST /api/v1/kiosk/logout`, QR y validaciones bajo `/api/v1/kiosk/...`.

## 3. Autenticación y autorización

- `POST /api/v1/auth/login` devuelve `token`, `refreshToken`, `user` y `branches[]`.
- `GET /api/v1/auth/me` devuelve `user` y `branches[]`.
- `POST /api/v1/auth/refresh` rota el refresh token.
- `POST /api/v1/auth/logout` invalida el refresh token.

`AuthUser` es global:

```json
{ "id": "...", "name": "...", "email": "...", "platformAdmin": true, "defaultBranchId": "..." }
```

`BranchAccess`:

```json
{ "id": "...", "name": "Sucursal Demo", "code": "sucursal-demo", "roles": ["owner"] }
```

Un `platformAdmin` puede autenticarse sin sucursales. Los roles operativos son por sucursal: `owner`, `trainer`, `member`. `admin` es global.

## 4. Estructura de paquetes

- `security/`: JWT, refresh, autorización, `AdminBootstrap`, filtros.
- `modules/branches/`: sucursales y códigos.
- `modules/site/`: settings, landing, media y contrato público.
- `modules/members/`: expedientes y fotos privadas.
- `modules/staff/`: vínculos de staff.
- `modules/plans/`, `payments/`: planes y pagos manuales.
- `modules/calendar/`: clases, plantillas y reservas.
- `modules/routines/`: rutinas, días y ejercicios.
- `modules/pos/`: productos, ventas e ítems.
- `modules/checkin/`: QR, códigos, dispositivos y auditoría.
- `modules/files/`: almacenamiento local y entrega de imágenes.
- `shared/`: `BaseCRUD`, `BaseService`, auditoría, errores y persistencia.
- `jobs/`, `notifications/`, `audit/`: capacidades de soporte.

## 5. Contratos esenciales

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "...", "password": "..." }
```

### Sucursales

```http
GET    /api/v1/branches
POST   /api/v1/branches
GET    /api/v1/branches/{id}
PATCH  /api/v1/branches/{id}
DELETE /api/v1/branches/{id}
```

Solo `platformAdmin`. `DELETE` archiva; no borra físicamente.

### Landing pública

```http
GET /api/v1/public/site
```

Respuesta:

```json
{
  "site": {},
  "theme": { "preset": "crossfit-dark", "overrides": {} },
  "blocks": [],
  "mediaAssets": []
}
```

### Miembros

```http
POST /api/v1/branches/{branchId}/members
{ "userId": "UUID_EXISTENTE", "code": "M-001", "active": true }
```

No crea cuentas globales. Solo vincula un `userId` activo.

### Fotos de miembros

```http
GET    /api/v1/branches/{branchId}/members/{memberId}/photo
POST   /api/v1/branches/{branchId}/members/{memberId}/photo
DELETE /api/v1/branches/{branchId}/members/{memberId}/photo
```

Requieren Bearer. La foto se guarda en `member_photos` y en almacenamiento local bajo `member-photos/{branchId}`.

### Pagos

```http
GET  /api/v1/branches/{branchId}/payments
POST /api/v1/branches/{branchId}/payments
```

Pagos manuales. No hay pasarela ni conciliación bancaria.

### Ventas

```http
GET  /api/v1/branches/{branchId}/sales
POST /api/v1/branches/{branchId}/sales
```

Las ventas incluyen `items[]` con `productId`, `productName`, `quantity`, `unitPrice` y `lineTotal`. El servidor calcula precios; nunca confía en el cliente.

## 6. Base de datos y migraciones

- Baseline: `src/main/resources/db/schema/schema.sql`.
- Módulos globales: `src/main/resources/db/schema/site_modules.sql`.
- Seed: `src/main/resources/db/seed/seed.sql`.
- Administrador inicial: `src/main/resources/db/seed/seed_platform_admin.sql`.
- Idempotencia: `src/main/resources/db/schema/idempotency_operations.sql`.
- Changelog: `src/main/resources/db/changelog-master.yaml`.

Reglas:

- El baseline es inmutable una vez aplicado. Los cambios posteriores van en changesets nuevos.
- El seed es idempotente: “Sucursal Demo”, settings del sitio, 3 imágenes externas y 4 bloques iniciales.
- No se incluyen contraseñas en SQL.
- `AdminBootstrap` crea el administrador con `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres) y lo vincula como `owner` de “Sucursal Demo” si existe.
- `spring.jpa.hibernate.ddl-auto=validate` debe pasar contra el esquema de Liquibase.
- Flyway está deshabilitado por defecto. El test de integración asume un esquema preparado externamente.

## 7. Ejecutar y verificar

### Local

```bash
mvn -DskipTests=false test
mvn -Plocal spring-boot:run
```

Variables mínimas:

```sh
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gymback
DB_USER=gymback
DB_PASSWORD=...
schema_db=test
JWT_SECRET=...                 # >= 32 bytes
FRONTEND_ORIGIN=http://localhost:5173
GMS_FILES_ROOT=/var/lib/gms/files
```

Health: `GET /api/v1/health`.

### WildFly

1. `mvn clean package`.
2. Copiar el WAR plano a `$WILDFLY_HOME/standalone/deployments/`.
3. Configurar datasource JNDI `java:/jdbc/AppDS` y driver PostgreSQL.
4. Arrancar con `-Dspring.profiles.active=wildfly`.
5. Definir `schema_db`, `JWT_SECRET`, `FRONTEND_ORIGIN`, `PUBLIC_SITE_BASE_URL`, `CHECKIN_MEMBER_QR_SECRET`, `KIOSK_CSRF_SECRET`, `KIOSK_COOKIE_SECURE` y `GMS_FILES_ROOT`.
6. Verificar `GET http://host:8080/gms/api/v1/health`.

`src/main/webapp/WEB-INF/jboss-deployment-structure.xml` excluye `jpa`, `bean-validation`, `weld`, `jsf` y `logging`; no eliminarlo sin revisar versiones.

## 8. Validación conocida

- `mvn test`: 78 tests, 0 fallos, 17 omitidos.
- `KioskPostgresIntegrationTest` con PostgreSQL 15: 21 tests, 0 fallos, 1 omitido.
- Esquema, seed e idempotencia aplicados en base limpia.
- Arranque local con `ddl-auto=validate` y `AdminBootstrap` verificado.
- Endpoints públicos y autenticados de sitio, media, sucursales, miembros y fotos verificados por HTTP.
