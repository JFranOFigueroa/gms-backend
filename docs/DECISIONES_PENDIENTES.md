# Decisiones confirmadas y pendientes

## Confirmadas

- El sitio es global y tiene una sola landing pública.
- Las sucursales son ubicaciones operativas; todos los recursos de negocio se filtran por `branch_id`.
- El seed crea “Sucursal Demo”, settings del sitio, imágenes y bloques iniciales.
- La cuenta administradora se crea con `AdminBootstrap`; no se incluyen contraseñas en SQL.
- Los pagos son manuales y administrativos; no hay pasarela ni conciliación bancaria.
- Las fotos de miembros son privadas y viven en `member_photos`.
- El kiosco se identifica por el código de sucursal (`?branch=<code>`).
- El esquema se gestiona con Liquibase; Flyway está deshabilitado por defecto.

## Pendientes

1. Matriz definitiva de permisos por rol y operación.
2. Registro público, invitaciones y recuperación de contraseña.
3. Flujo multirrol cuando un usuario es `owner` y `trainer` en la misma sucursal.
4. Reglas definitivas de reservas, invitados y límites por categoría.
5. Despliegue validado en WildFly real.
6. Pruebas E2E de navegador para landing, dashboard y kiosco.
7. Sustitución de `alert()`/`confirm()` nativos en el frontend.
8. Revisión de accesibilidad y contraste de temas.

## Exclusiones actuales

- No hay landings por sucursal ni enlaces `/g/{slug}`.
- No hay publicación manual de landing.
- No hay pasarela de pagos, reembolsos ni conciliación.
- No hay almacenamiento cloud; las imágenes se guardan en disco local.
- No hay notificaciones persistentes ni workers.
