# Gym Back — backend Java

**Punto de entrada local:** `src/main/java/com/gymback/GymBackApplication.java`.
**Punto de entrada WildFly:** `src/main/java/com/gymback/ServletInitializer.java`.
**Arquitectura de referencia:** [README_MASTER_BACKEND.md](README_MASTER_BACKEND.md).

Backend Java 21 + Spring Boot 4.0.9 + PostgreSQL. No incorpora el frontend.

## Modelo actual

- **Sitio**: marca global. Configuración, tema, landing y multimedia viven en tablas `site_*`.
- **Sucursal**: unidad operativa. Miembros, staff, clases, rutinas, POS y check-in se aíslan por `branch_id`.
- **Una sola landing pública**: `GET /api/v1/public/site`. No hay endpoints por gimnasio ni slugs de landing.
- **Kiosco**: se activa con el código de sucursal (`POST /api/v1/kiosk/activate`).

## Base de datos

El esquema se gestiona externamente con **Liquibase**. La aplicación no ejecuta Flyway por defecto.

Archivos:

- `src/main/resources/db/schema/schema.sql`: baseline consolidado de sitio, sucursales, auditoría y `member_photos`.
- `src/main/resources/db/schema/site_modules.sql`: modo de módulos globales del sitio (`modules_global`, `global_modules`).
- `src/main/resources/db/seed/seed.sql`: seed idempotente de “Sucursal Demo”, settings del sitio, imágenes y bloques iniciales.
- `src/main/resources/db/seed/seed_platform_admin.sql`: cuenta inicial del administrador del sistema y su rol global.
- `src/main/resources/db/schema/idempotency_operations.sql`: tabla de idempotencia de operaciones.
- `src/main/resources/db/changelog-master.yaml`: changelog raíz.

```bash
cp liquibase.properties.example liquibase.properties
# Editar url, username, password y schema_db

liquibase --defaults-file=liquibase.properties validate
liquibase --defaults-file=liquibase.properties update-sql
liquibase --defaults-file=liquibase.properties update
```

- `liquibase.properties` está ignorado por Git; no versionar credenciales.
- El baseline está pensado para una base nueva o vacía. Si el esquema ya existe, usar `changelog-sync` o evaluar la transición con un DBA.
- El seed de sucursal/sitio no contiene contraseñas. El changeset `seed_platform_admin.sql` crea la cuenta administradora inicial con contraseña de arranque; debe cambiarse fuera de la UI. `AdminBootstrap` solo complementa cuentas sin contraseña.

## Desarrollo local

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL 15+.

```bash
mvn -DskipTests=false test
mvn -Plocal spring-boot:run
```

El perfil `local` usa Tomcat embebido y un datasource JDBC/Hikari. Flyway está deshabilitado por defecto (`LOCAL_FLYWAY_ENABLED=false`); el esquema debe existir previamente.

También se puede generar el WAR ejecutable:

```bash
mvn clean package
java -jar target/gym-back-1.0.0-SNAPSHOT-exec.war
```

## Despliegue en WildFly 40 (Jakarta EE 11)

Objetivo: **WildFly 40.0.1.Final-jdk21 estándar** (Jakarta EE 11, Servlet 6.1, JPA 3.2, Bean Validation 3.1).

### Build

```bash
mvn clean package
```

Genera:

- `target/gym-back-1.0.0-SNAPSHOT.war` → WAR plano para WildFly.
- `target/gym-back-1.0.0-SNAPSHOT-exec.war` → WAR ejecutable para local.

### Datasource

La aplicación usa el datasource JNDI configurado en WildFly:

```yaml
spring:
  datasource:
    jndi-name: java:/jdbc/AppDS
```

Requisitos:

- Driver PostgreSQL instalado como módulo.
- Datasource `java:/jdbc/AppDS` habilitado.
- Si el esquema no es `public`, añadir `currentSchema` a la URL JDBC o:

```xml
<new-connection-sql>SET search_path TO test, public</new-connection-sql>
```

### Perfil y variables

Arrancar WildFly con:

```bash
-Dspring.profiles.active=wildfly
```

Variables/sistema necesarios:

```bash
-Dschema_db=test
-DJWT_SECRET=...                 # >= 32 bytes
-DFRONTEND_ORIGIN=https://...
-DPUBLIC_SITE_BASE_URL=https://...
-DCHECKIN_MEMBER_QR_SECRET=...
-DKIOSK_CSRF_SECRET=...
-DKIOSK_COOKIE_SECURE=true
-DGMS_FILES_ROOT=/var/lib/gms/files
```

Opcionales:

- `BOOTSTRAP_ADMIN_EMAIL` (por defecto `admin@gms.local`).
- `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres).
- `USER_API_ENABLED`.

`AdminBootstrap` solo crea el administrador si `BOOTSTRAP_ADMIN_PASSWORD` está definido y la cuenta no existe. Si el seed ya creó `admin@gms.local`, no sobrescribe su contraseña. También lo vincula como `owner` de “Sucursal Demo” si la sucursal existe.

### Context root

El WAR publica la aplicación en `/gms`:

- Health: `GET http://host:8080/gms/api/v1/health`
- API: `http://host:8080/gms/api/v1/...`

### Classloading

`src/main/webapp/WEB-INF/jboss-deployment-structure.xml` excluye los subsistemas `jpa`, `bean-validation`, `weld`, `jsf` y `logging`. Spring Boot usa su propio Hibernate, Bean Validation, Jackson 3 y Logback desde `WEB-INF/lib`. No eliminar ese archivo sin revisar conflictos de versiones.

La exclusión de `logging` es obligatoria: sin ella WildFly impone su módulo `org.slf4j` y Logback deja de ser el binding.

### `ddl-auto`

Se mantiene `spring.jpa.hibernate.ddl-auto=validate`. Si el esquema de Liquibase no coincide con las entidades JPA, el despliegue falla. Corregir esquema/entidades o usar `none` temporalmente.

## Validación realizada

- `mvn test`: 78 tests, 0 fallos, 17 omitidos (integración PostgreSQL opt-in).
- `KioskPostgresIntegrationTest` con PostgreSQL 15: 21 tests, 0 fallos, 1 omitido.
- `schema.sql`, `site_modules.sql`, `seed.sql`, `seed_platform_admin.sql` e `idempotency_operations.sql` aplicados en una base limpia; seeds reejecutados sin duplicados.
- La cuenta `admin@gms.local` del seed se validó como `active`, con hash BCrypt verificable y rol en `platform_admins`.
- Arranque local con `ddl-auto=validate`, `AdminBootstrap`, landing pública, settings, media y fotos privadas verificados por HTTP.
