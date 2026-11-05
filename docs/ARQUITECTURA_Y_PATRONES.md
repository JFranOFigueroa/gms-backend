> **Patrón vigente (refactor JPA 03/11/2026):** Repositorios con JPA/JPQL y modelos mapeados; `BaseCRUD` para el CRUD común, JPA especializado para agregados/consultas. Solo `AuditActorRepository` usa una consulta PostgreSQL `set_config` para triggers. No hay `JdbcTemplate`. Los pasajes inferiores que permitían SQL directo son históricos. Leer `docs/REFACTORIZACION_JPA_BUENAS_PRACTICAS.md`.

> **Decisión vigente:** la página es automática por gimnasio y se consulta por slug. No existe flujo manual de publicar/despublicar. Véase `docs/DECISION_LANDING_ENLACE_DIRECTO.md` para los contratos actuales; las referencias históricas a snapshots/publicación describen la integración anterior.

# Arquitectura y patrón obligatorio

```
com.gymback
  GymBackApplication.java   <-- main Spring Boot
  shared/
    domain/BaseEntity.java
    interfaces/IBaseCrudRepository.java
    repositories/BaseCRUD.java
    services/BaseService.java, AuditService.java
    factories/ServiceFactory.java
    api/{HealthController,ApiExceptionHandler}.java
  security/{SecurityConfig,AuthorizationService,AuthService,IdentityRepository,...}
  modules/
    gyms/        {model,interfaces,repositories,services,dto,routes}
    members/     {model,interfaces,repositories,services,dto,routes}
    staff/       {interfaces,repositories,services,dto,routes}
    plans/       {model,interfaces,repositories,services,dto,routes}
    payments/    {interfaces,repositories,services,dto,routes}
    calendar/    {model,interfaces,repositories,services,dto,routes}
    routines/    {model,interfaces,repositories,services,dto,routes}
    pos/         {model,interfaces,repositories,services,dto,routes}
    checkin/     {interfaces,repositories,services,dto,routes}
    settings/    {interfaces,repositories,services,dto,routes}
    landing/     {model,interfaces,repositories,services,dto,routes}
    media/       {model,interfaces,repositories,services,dto,routes}
    notifications/, jobs/ (reservado); files/ y audit/ implementados en V4
```

## Correspondencia Python → Java

| Patrón de `app.zip` | Implementación Java | Responsabilidad |
|---|---|---|
| `Protocol IOrdersRepository` | `I*Repository extends IBaseCrudRepository<Entity>` | contrato sin SQL |
| `BaseCRUD[Model]` | `shared.repositories.BaseCRUD<T>` | CRUD JPA genérico, filtro `gymId`, paginación, excluir deletedAt cuando exista |
| `NotificationsRepository(BaseCRUD, Interface)` | `*Repository extends BaseCRUD<Entity> implements I*Repository` | JPA/JPQL, consultas especializadas |
| `BaseService` | `shared.services.BaseService<T>` | operaciones compartidas, errores 404, auditoría de campos existentes |
| `set_audit_fields` | `AuditService` | timestamps + actor si las columnas existen |
| `repository_factory.py` | `shared.factories.ServiceFactory` | inyección de servicios con interfaces |
| `routes/*_handler.py` | `modules/*/routes/*Controller` | HTTP, DTO, status, **sin** lógica de negocio |
| `SqlAlchemyUnitOfWork` | `@Transactional` Spring | transacción por caso de uso, rollback automático |

El equivalente de `get_where` es `getWhere(gymId, Specification<T>, offset, limit)` (Criteria JPA tipado, siempre con tenant y soft-delete); no se aceptan nombres de columnas dinámicos desde HTTP. `get_all` se reemplaza por `list(gymId, offset, limit)` para evitar lecturas sin límite.

## Prohibiciones

- No acceso al `EntityManager` o `JdbcTemplate` desde controladores ni services.
- No actualizar todo el entity desde JSON de usuario (`ObjectMapper.readerForUpdating` / mass assignment).
- No confiar en `gymId`, `userId`, montos, totales ni rol aportados por el frontend; `gymId` se verifica, el actor sale de JWT, totales se derivan de tablas.
- No cruzar gimnasios usando un lookup por ID sin `gymId`.
- No `DROP`/borrado físico de pagos, ventas o auditorías.
- No saltarse `@Transactional` en flujos de varios repositorios.
- Para entidad sin `deleted_at`: estado inactivo/cancelled si hay dependencias; borrado físico solo donde el SQL y la regla lo permiten.

## Ejemplo para copiar en nuevos módulos

1. `model/Foo` mapea **solo** columnas existentes.
2. `interfaces/IFooRepository extends IBaseCrudRepository<Foo>`.
3. `repositories/FooRepository extends BaseCRUD<Foo> implements IFooRepository`.
4. `dto/FooRequest` tipa campos mutables y valida solicitudes.
5. `services/FooService extends BaseService<Foo>` valida reglas, acceso, y coordina repos.
6. `routes/FooController` delega; `ServiceFactory` conecta las dependencias.
7. Prueba tenant, roles, reglas de negocio, constraints, y contrato frontend.

## Auditoría

**Actualización V4:** todas las tablas del esquema existente reciben `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`; `AuditService` y `AuditContext` atribuyen cambios a actor, y triggers PostgreSQL cubren JPA y JDBC. `BaseCRUD`, consultas directas y vistas filtran eliminados. `audit_events` se escribe por triggers sin snapshots sensibles y se consulta con endpoints autorizados. Ver `docs/CAMBIOS_IMPLEMENTADOS_AUDITORIA.md` para detalle y excepciones históricas.


## Regla reforzada (revisión 2026-11-03)

- Las cuentas globales son responsabilidad de provisión SQL; `MemberService` y `StaffService` solo enlazan UUID existente y activo mediante `IIdentityRepository`. Ni el controller ni el repository de miembro deben crear usuarios.
- `AuthService` arma `AuthSession` único; DTO de token separado para login. `IdentityRepository` resuelve `GymAccess.roles` por gimnasio. No usar un rol global ficticio.
- Los servicios administran reglas; `ISaleRepository` entrega snapshots y líneas, y `SaleService` devuelve `SaleView` en GET/POST, sin consultas N+1.
- `RoutineService` emplea `IRoutineRepository.getWhere` con `Specification` para filtrar `published=true` antes de paginar, no filtra listas luego de paginar.
- `ClassCategories` es fuente canónica de categorías permitidas por Java; mantener sincronía con Flyway V2. La regla provisional `functional` como clase regular afecta repositorio de reservas y Service.
- La matriz de permisos y las políticas de staff multirrol no están aprobadas: no inventar nuevas facultades; bloquear cambios ambiguos en lugar de adivinar.

## Extensiones de integración del 03/11/2026

Se añadieron `modules/landing/{routes,services,interfaces,repositories}` para gestión de enlace y lectura anónima de los bloques guardados; `modules/users/` y servicios de registro guiado, protegidos por bandera de despliegue; y `modules/dashboard/` para métricas agregadas. La consulta pública no invoca `AuthorizationService.actor()` y está limitada a gimnasios activos con bloques habilitados. El estado histórico `landing_published` no se consulta. Controllers delegan a services, services a interfaces, repositorios a JPA/JPQL. No realizar SQL en Controller/Service.
