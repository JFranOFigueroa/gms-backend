# Gym Back — backend Java (actualizado)

**Primero leer:** [README_MASTER_BACKEND.md](README_MASTER_BACKEND.md). **Punto de entrada:** `src/main/java/com/gymback/GymBackApplication.java`.

Proyecto backend solamente, Java 21 + Spring Boot 3.5.7 + PostgreSQL. No incorpora el frontend. El usuario puede elegir uno de sus gimnasios durante el flujo de login, con roles propios por gimnasio. Cuentas globales se crearán por SQL; los endpoints de miembros/staff solo enlazan cuentas ya existentes. Pagos solo manuales.

`schema_db=test` selecciona esquema. Flyway usa V1 + V2. Para ejecutar: configurar `.env`; `mvn clean test`; `mvn spring-boot:run` o `docker compose up --build`.

**Importante:** no se ha compilado ni probado sobre una BD PostgreSQL real en este entorno; consultar `docs/VERIFICACION.md` antes de desplegar.
