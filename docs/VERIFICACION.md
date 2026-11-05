# Verificación pendiente y pruebas iniciales

## Qué se ha comprobado en esta entrega

- Inspección estática de archivos de Java y de migraciones; verificación de rutas sin `{{...}}`.
- Se agregaron tests unitarios para contrato login/me y roles multi-gym, asociación de usuarios existentes, rechazo de cambios de identidad, rutinas privadas, venta con líneas y categoría functional.
- **No se ha corrido Maven/JUnit ni PostgreSQL aquí.** No asumir que la aplicación ya compiló, migró o pasó pruebas de integración reales.

## Comandos de validación en máquina con Maven y PostgreSQL

```bash
mvn clean test
mvn spring-boot:run
curl http://localhost:8080/api/v1/health
```

O con Docker y secretos configurados: `docker compose up --build`.

## Pruebas HTTP y BD mínimas, antes de conectar Vue

1. BD PostgreSQL 15+ vacía con `schema_db=qa`: Flyway V1 y V2 completan, `class_templates`/`gym_classes` admiten `functional` y mantienen sus otros CHECK; no se crean accidentalmente tablas en `test`.
2. Usuario global activo preinsertado manualmente, con hash BCrypt correcto; login válido, inválido y disabled. `POST /auth/login` devuelve `user + gyms[roles[]]`; `GET /auth/me` conserva exactamente esos datos sin `token`.
3. Usuario owner en gimnasio A, member en B y trainer en C; selector contiene los tres roles efectivos. Admin global con y sin asignación staff ve los gimnasios activos. Gimnasio suspendido NO se ofrece.
4. `defaultGymId` inactivo o revocado: `/auth/me` devuelve null para ese campo, sin habilitar acceso implícito.
5. Usuario global sin relación al gimnasio no puede acceder a endpoints tenant; B no lee ni cambia miembros, ventas, pagos ni rutinas de A.
6. `POST /members` y `/staff`: `userId` existente y activo funciona; usuario inexistente/inactivo falla; no se inserta ninguna fila nueva en `users`; PATCH con `userId` falla.
7. Múltiples roles staff: `GET /staff` lista `roles[]` por usuario sin duplicarlo; `PATCH /staff/{userId}` con roles simultáneos falla sin cambios; owner no puede eliminar rol owner ajeno, con o sin segundo rol.
8. `GET /routines` para miembro usa filtro `published=true` antes de paginación; `GET /routines/{id}` borrador retorna 404; owner/trainer/admin pueden acceder según autorización provisional.
9. `GET /sales`: cada venta contiene `items[]` (productId, nombre snapshot, cantidad, precios). `POST /sales` devuelve detalle persistido. Prueba múltiples ventas sin N+1, stock insuficiente y dos ventas concurrentes sobre última unidad.
10. Crear plantilla/clase `functional` con POST, y comprobar reserva de categoría regular cuando existe otra clase normal del mismo día.
11. Pagos manuales: monto deriva del plan, registra método, actualiza fecha de vencimiento, no acepta ni ejecuta cargos bancarios; verificar rollback si hay conflicto.
12. Revisión de permisos fina sigue **pendiente de negocio**. No publicar esta API como lista para producción sin esa matriz validada.

## Estado

Los tests unitarios son una salvaguarda inicial, NO reemplazan `mvn test` y pruebas de integración con una BD real. Además falta verificar build Docker y endpoints reales. Las funciones pendientes no se simulan ni se dan por terminadas.


## Compilación corregida (2026-11-03)

Se corrigieron las tres fallas de compilación reportadas en PowerShell (`JdbcTemplate.query` ambiguo dos veces y ausencia de `IIdentityRepository.insertPlatformAdmin`). En el equipo local ejecutar `mvn clean test` y después `mvn spring-boot:run`. Esta corrección no equivale a afirmar una compilación Maven completada en el entorno de generación.

## Verificación para integración nueva (03/11/2026)

Se verificó sintaxis de 152 archivos Java mediante `JavacTask.parse()` (0 errores) y compilación aislada de `SlugPolicy` con Java 21. Se añadieron pruebas unitarias de slug, publicación de landing y creación global protegida por flag, **sin ejecutarlas** porque no hay Maven/pom del proyecto raíz en el ZIP suministrado. Continúan pendientes: `mvn clean test`, ejecutar Flyway V3 sobre PostgreSQL real, verificar L01-L05 y aislamiento por gimnasio, validar GET de reservas/checkins/miembros propios y dashboard, probar bandera de alta apagada/encendida y CORS. No indicar integración completa/producción hasta obtener pruebas HTTP y BD.


## Pruebas vigentes de landing por enlace (sin publicación manual)

1. `GET /api/v1/public/gyms/{slug}/landing` responde sin JWT con bloques habilitados, ordenados y contenidos actuales del gimnasio activo.
2. Cambiar/guardar texto, orden o tema desde el builder aparece en la siguiente consulta pública sin botón adicional.
3. `PATCH /api/v1/gyms/{gymId}/landing/slug` respeta permisos multi-gym y devuelve 409 cuando el slug ya existe.
4. `GET /api/v1/gyms/{gymId}/landing/link` entrega el enlace; un gimnasio suspendido no abre la página.
5. Sin `POST /landing/publish` ni `POST /landing/unpublish`; sus rutas no deben estar registradas.
6. No borrar `V3__landing_publications.sql` si ya se aplicó: ejecutar Flyway en bases nuevas y existentes.


## Verificación V4 auditoría/refresh/archivos

Código y checklist: `docs/CAMBIOS_IMPLEMENTADOS_AUDITORIA.md`. La sintaxis de Java 21 fue revisada sobre fuentes de main/test (sin diagnósticos). **No se ejecutó Maven ni PostgreSQL**, el ZIP original no aporta `pom.xml`, por lo que ejecutar los tests nuevos `RefreshTokenTest` y `LocalImageStorageTest`, aplicar V4 en staging, confirmar permisos de `/var/lib/gms/files` y verificar roles/tenant.
