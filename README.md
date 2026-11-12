# Gym Back — backend Java

**Primero leer:** [README_MASTER_BACKEND.md](README_MASTER_BACKEND.md). **Punto de entrada local:** `src/main/java/com/gymback/GymBackApplication.java`. **Punto de entrada WildFly:** `src/main/java/com/gymback/ServletInitializer.java`.

Proyecto backend solamente, Java 21 + Spring Boot 4.0.9 + PostgreSQL. No incorpora el frontend. El usuario puede elegir uno de sus gimnasios durante el flujo de login, con roles propios por gimnasio. Cuentas globales se crearán por SQL; los endpoints de miembros/staff solo enlazan cuentas ya existentes. Pagos solo manuales.

La base de datos se gestiona externamente con Liquibase. La aplicación **no ejecuta Flyway** en WildFly (`spring.flyway.enabled=false`). Las migraciones V1–V6 se conservan como referencia y para el test de integración opcional.

## Liquibase

El baseline del esquema está en `src/main/resources/db/schema/schema.sql` (formatted SQL consolidado de V1–V6). El changelog raíz es `src/main/resources/db/changelog-master.yaml` e incluye `src/main/resources/db/seed/seed.sql`.

```bash
cp liquibase.properties.example liquibase.properties
# Editar url, username, password y schema_db en liquibase.properties

liquibase --defaults-file=liquibase.properties validate
liquibase --defaults-file=liquibase.properties update-sql
liquibase --defaults-file=liquibase.properties update
```

- `liquibase.properties` está ignorado por Git; no versionar credenciales.
- Con la configuración de ejemplo, las tablas de control de Liquibase viven en `public` y el primer changeset crea el esquema `${schema_db}`.
- Si prefieres que `databasechangelog` viva en `${schema_db}`, crea el esquema antes de ejecutar y ajusta `defaultSchemaName`/`liquibaseSchemaName`.
- El baseline está pensado para una base nueva o vacía. Si el esquema ya existe, no ejecutes `update` directamente: usa `changelog-sync` o evalúa la transición con un DBA.
- `db/migration/V1–V6` permanece sin cambios como referencia y para el test Flyway opcional.

## Desarrollo local

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL.

```bash
# .env con DB_*, schema_db, JWT_SECRET, FRONTEND_ORIGIN...
mvn -DskipTests=false test
mvn -Plocal spring-boot:run
```

El perfil `local` usa Tomcat embebido y un datasource JDBC/Hikari. `schema_db` (alias `SCHEMA_DB`) selecciona el esquema PostgreSQL.

También se puede generar el WAR ejecutable. `mvn clean package` ya produce un `*-exec.war` con Tomcat y PostgreSQL en `WEB-INF/lib-provided`, listo para `java -jar`:

```bash
mvn clean package
java -jar target/gym-back-1.0.0-SNAPSHOT-exec.war
```

El perfil `-Plocal` solo es necesario para `spring-boot:run`, porque deja Tomcat/PostgreSQL en scope `runtime` para el plugin de Maven.

## Despliegue en WildFly 40 (Jakarta EE 11)

Objetivo: **WildFly 40.0.1.Final-jdk21 estándar** (Jakarta EE 11, Servlet 6.1, JPA 3.2, Bean Validation 3.1). Spring Boot 4.0.9 tiene como baseline Jakarta EE 11, por lo que el stack coincide con el servidor.

### Build

```bash
mvn clean package
```

Genera:

- `target/gym-back-1.0.0-SNAPSHOT.war` → WAR plano para WildFly (sin Tomcat ni driver PostgreSQL).
- `target/gym-back-1.0.0-SNAPSHOT-exec.war` → WAR ejecutable para local (Tomcat y PostgreSQL en `WEB-INF/lib-provided`). Se genera con el mismo `mvn clean package`.

Desplegar el WAR plano:

```bash
cp target/gym-back-1.0.0-SNAPSHOT.war "$WILDFLY_HOME/standalone/deployments/"
```

### Datasource

La aplicación usa el datasource JNDI configurado en WildFly:

```yaml
spring:
  datasource:
    jndi-name: java:/jdbc/AppDS
```

Requisitos en WildFly:

- Driver PostgreSQL instalado como módulo.
- Datasource `java:/jdbc/AppDS` habilitado.
- Si el esquema no es `public`, añadir al datasource:

```xml
<new-connection-sql>SET search_path TO test, public</new-connection-sql>
```

o `currentSchema=test` en la URL JDBC.

### Perfil y variables

Arrancar WildFly con el perfil `wildfly`:

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

Opcionales: `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `USER_API_ENABLED`.

Se pueden definir en `standalone.conf`, `setenv.sh` o en `<system-properties>` de `standalone.xml`.

### Context root

El WAR publica la aplicación en `/gms`:

- Health: `GET http://host:8080/gms/api/v1/health`
- API: `http://host:8080/gms/api/v1/...`

El frontend debe usar ese prefijo.

### Classloading

`src/main/webapp/WEB-INF/jboss-deployment-structure.xml` excluye los subsistemas `jpa`, `bean-validation`, `weld`, `jsf` y `logging`. Spring Boot usa su propio Hibernate, Bean Validation, Jackson 3 y Logback desde `WEB-INF/lib`; la aplicación no usa CDI ni JSF. No eliminar ese archivo sin revisar los conflictos de versiones.

La exclusión de `logging` es obligatoria: sin ella WildFly impone su módulo `org.slf4j` (`slf4j-jboss-logmanager`) con prioridad sobre `WEB-INF/lib`, Logback deja de ser el binding y Spring Boot falla con `LoggerFactory is not a Logback LoggerContext`. Toda la aplicación usa **Jackson 3** (`tools.jackson.*`), que es el que auto-configura Spring Boot 4; no hay ningún bean manual de Jackson 2.

Si despliegas desde IntelliJ con un artefacto **exploded**, haz `Build > Rebuild Project` y después **Redeploy**. Los cambios en `WEB-INF/*.xml` no se aplican con hot swap.

### Nota sobre `ddl-auto`

Se mantiene `spring.jpa.hibernate.ddl-auto=validate`. Si el esquema creado por Liquibase no coincide exactamente con las entidades JPA, el despliegue fallará; en ese caso hay que corregir el esquema/entidades o usar `none` temporalmente.
