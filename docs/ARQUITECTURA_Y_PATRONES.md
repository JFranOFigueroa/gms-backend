# Arquitectura y patrones

## Estructura

```text
com.gymback
  GymBackApplication.java
  shared/
    domain/BaseEntity.java
    interfaces/IBaseCrudRepository.java
    repositories/BaseCRUD.java
    services/BaseService.java, AuditService.java
    factories/ServiceFactory.java
    api/{HealthController,ApiExceptionHandler}.java
  security/{SecurityConfig,AuthorizationService,AuthService,IdentityRepository,...}
  modules/
    branches/     {model,interfaces,repositories,services,dto,routes}
    site/         {model,interfaces,repositories,services,dto,routes}
    members/      {model,interfaces,repositories,services,dto,routes}
    staff/        {interfaces,repositories,services,dto,routes}
    plans/        {model,interfaces,repositories,services,dto,routes}
    payments/     {interfaces,repositories,services,dto,routes}
    calendar/     {model,interfaces,repositories,services,dto,routes}
    routines/     {model,interfaces,repositories,services,dto,routes}
    pos/          {model,interfaces,repositories,services,dto,routes}
    checkin/      {interfaces,repositories,services,dto,routes}
    settings/     {interfaces,repositories,services,dto,routes}
    files/        {services,routes}
    audit/        {interfaces,repositories,services,routes}
```

## Capas

```text
Controller -> Service -> I*Repository -> Repository -> PostgreSQL
```

- **Controller**: HTTP, DTO, status. Sin lógica de negocio.
- **Service**: reglas, autorización, transacciones, coordinación.
- **Interface**: contrato del repositorio.
- **Repository**: JPA/JPQL, consultas especializadas.
- **PostgreSQL**: constraints, triggers de auditoría y soft delete.

## Patrones

- `BaseCRUD<T>`: CRUD genérico, filtro tenant y soft delete.
- `BaseService<T>`: operaciones compartidas, 404 y auditoría.
- `AuditService`: campos de auditoría y contexto de actor.
- `ServiceFactory`: ensamblado de servicios.
- `@Transactional`: transacción por caso de uso.

## Reglas

- No acceder a `EntityManager` desde controllers o services.
- No actualizar entidades completas desde JSON.
- No confiar en `branchId`, `userId`, montos, totales ni roles enviados por el cliente.
- El actor sale del JWT.
- No cruzar sucursales sin validar `branch_id`.
- No borrar físicamente pagos, ventas ni auditoría.
- No saltarse transacciones en flujos multi-repositorio.
- Los módulos globales del sitio no usan `branch_id`; los operativos sí.

## Sitio vs sucursal

- `modules/site`: settings, landing y media globales.
- `modules/branches`: sucursales y códigos.
- El resto de módulos operativos reciben `branchId` y validan permisos.
- `AuthorizationService.siteAdmin()` permite `platformAdmin` u `owner` de alguna sucursal.
- `AuthorizationService.require(branchId, roles...)` valida la sucursal y el rol.

## Auditoría

- Triggers PostgreSQL escriben `audit_events`.
- `AuditActorRepository` usa `set_config` para el actor.
- `BaseCRUD` y las consultas filtran `deleted_at`.
- No se exponen snapshots sensibles.

## Nuevos módulos

1. `model/Foo` mapea solo columnas existentes.
2. `interfaces/IFooRepository extends IBaseCrudRepository<Foo>`.
3. `repositories/FooRepository extends BaseCRUD<Foo>`.
4. `dto/FooRequest` tipa campos mutables.
5. `services/FooService` valida reglas y permisos.
6. `routes/FooController` delega.
7. `ServiceFactory` conecta dependencias.
8. Pruebas de tenant, roles, reglas y contrato.
