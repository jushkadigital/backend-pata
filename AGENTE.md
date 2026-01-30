# AGENTE.md

Este archivo proporciona contexto detallado sobre el proyecto para asistentes de IA (Claude Code, OpenAI, etc.)

## Visión General

Aplicación **Quarkus 3.26.3** multi-módulo construida con **Arquitectura Hexagonal** y patrones **DDD** (Domain-Driven Design).

## Stack Tecnológico

- **Java 21**
- **Maven** para gestión de dependencias y builds
- **PostgreSQL** con Hibernate Panache y Flyway para migraciones
- **Keycloak 26** para autenticación (OIDC)
- **HashiCorp Vault** para gestión de secretos
- **Vert.x EventBus** para mensajería entre servicios
- **OpenAPI Generator 7.8** para generar interfaces REST
- **MapStruct** para mapeo entre capas
- **Lombok** para reducir código repetitivo

## Estructura del Proyecto

```
bootloader/          # Punto de entrada Quarkus, agrega todos los servicios
user/
├── iam/             # Identity & Access Management (integración Keycloak)
│   ├── domain/              # Entidades, value objects, interfaces de repositorio
│   ├── application/         # Casos de uso, comandos, servicios
│   ├── infrastructure/     # Implementaciones de adaptadores
│   │   ├── adapter-postgresql/
│   │   ├── adapter-rest/    # Controladores REST (generados desde OpenAPI)
│   │   ├── adapter-keycloak/
│   │   ├── adapter-eventbus/
│   │   └── adapter-webhook/
│   └── bootstrap/           # Configuración CDI y startup
├── passenger/       # Gestión de perfiles de pasajeros
│   └── (misma estructura que iam/)
└── admin/           # Gestión de usuarios administradores
    └── (misma estructura que iam/)
```

## Patrones de Arquitectura

### Arquitectura Hexagonal

Cada módulo sigue la arquitectura hexagonal con capas bien definidas:

1. **domain/** - Capa de dominio puro
   - Entidades (JPA entities)
   - Value objects
   - Interfaces de puertos (ej: `UserRepository`)

2. **application/** - Capa de aplicación
   - Casos de uso (use cases)
   - Comandos/DTOs
   - Servicios que orquestan lógica de dominio

3. **infrastructure/** - Adaptadores
   - `adapter-postgresql/` - Implementaciones de repositorio con Hibernate Panache
   - `adapter-rest/` - Controladores REST generados desde `api.yml` con OpenAPI Generator
   - `adapter-keycloak/` - Integración con Keycloak
   - `adapter-eventbus/` - Listeners y publishers de Vert.x EventBus
   - `adapter-webhook/` - Manejadores de webhooks

4. **bootstrap/** - Configuración de inicio
   - Beans CDI
   - Observadores de eventos de startup

### Patrones Clave

- **Repository Pattern**: El dominio define interfaces (`UserRepository`), la infraestructura las implementa
- **Event-Driven Communication**: Los módulos se comunican vía Vert.x EventBus
  - Topics: `iam.user.registered`, `iam.webhook.keycloak`, etc.
- **OpenAPI-First**: APIs REST generadas desde `adapter-rest/src/main/resources/api.yml`
- **MapStruct Mappers**: Mapeo de objetos entre capas
- **Dependency Inversion**: Las capas de dominio no dependen de las capas de infraestructura

## Secuencia de Inicio

Las clases Bootstrap observan `StartupEvent` con prioridad:

1. `IamBootstrap` (Prioridad 10) - Crea realm Keycloak, roles, webhooks
2. `PassengerBootstrap` (Prioridad 20)
3. `AdminBootstrap` (Prioridad 30)

## Comandos de Build

```bash
# Instalar dependencias y compilar
mvn clean install

# Modo desarrollo (hot reload + dev services)
mvn clean quarkus:dev -Dquarkus.http.port=8081 -Dquarkus.enableGlobal=false

# Package para producción
mvn clean package -pl bootloader

# Build de imagen nativa (requiere GraalVM)
mvn clean package -Dnative

# Ejecutar tests
mvn test

# Ejecutar una clase de test específica
mvn test -Dtest=TestClassName

# Ejecutar un método de test específico
mvn test -Dtest=TestClassName#methodName
```

## Base de Datos

- **Flyway migrations**: `user/*/infrastructure/adapter-postgresql/src/main/resources/db/migration/`
- **Schema**: `quarkus`
- **ORM**: Hibernate Panache

## Configuración

- Config principal: `bootloader/src/main/resources/application.yml`
- Config por módulo: `user/*/infrastructure/adapter-*/src/main/resources/application.yml`

## Notas de Desarrollo

- **Dev services**: Inician automáticamente contenedores de PostgreSQL, Keycloak y Vault
- **Docker**: Debe estar corriendo para usar dev services
- **Swagger UI**: `/q/swagger-ui` (solo en modo dev)
- **Hot reload**: Activado con `quarkus:dev`

## Convenciones de Código

- **Paquetes**: Seguir estructura hexagonal estricta
- **Nombres**: Usar nombres de negocio del dominio
- **Interfaces**: Definir en el dominio, implementar en infraestructura
- **Mappers**: Usar MapStruct con `@Mapper(componentModel = "cdi")`
- **Logging**: Usar `org.jboss.logging.Logger` de Quarkus
- **Testing**: Usar JUnit 5 + QuarkusTest

## Eventos de EventBus

Los módulos publican/consumen eventos vía Vert.x EventBus:

- `iam.user.registered` - Publicado cuando un usuario se registra
- `iam.webhook.keycloak` - Eventos webhook de Keycloak
- Otros eventos por módulo...

## Integración con Keycloak

- **OIDC Provider**: Configurado en `application.yml`
- **Webhooks**: Configurados en `IamBootstrap` durante startup
- **Token Management**: Vía extension `quarkus-oidc`

## Secrets Management

- **Vault**: Configurado para almacenar secretos (API keys, passwords, etc.)
- **Dev services**: Inicia automáticamente Vault en modo dev

## Testing

Los tests deben seguir la estructura del proyecto:

- Test unitarios: En cada paquete correspondiente
- Test de integración: En `src/test/java/`
- Usar `@QuarkusTest` para tests de integración Quarkus

## Recursos Útiles

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Hibernate Panache](https://quarkus.io/guides/hibernate-panache)
- [Vert.x EventBus](https://vertx.io/docs/apidocs/io/vertx/core/eventbus/EventBus.html)
- [OpenAPI Generator](https://openapi-generator.tech/docs/generators/java)
