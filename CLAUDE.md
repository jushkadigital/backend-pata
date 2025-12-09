# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Install dependencies and compile
mvn clean install

# Run in development mode (hot reload + dev services)
mvn clean quarkus:dev -Dquarkus.http.port=8081 -Dquarkus.enableGlobal=false

# Package for production
mvn clean package -pl bootloader

# Build native image (requires GraalVM)
mvn clean package -Dnative

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=TestClassName

# Run a single test method
mvn test -Dtest=TestClassName#methodName
```

## Architecture Overview

**Quarkus 3.26.3** multi-module application using **Hexagonal Architecture** with **DDD** patterns.

### Module Structure

```
bootloader/          # Quarkus entry point, aggregates all services
user/
├── iam/             # Identity & Access Management (Keycloak integration)
├── passenger/       # Passenger profile management
└── admin/           # Admin user management
```

### Layer Structure (per module)

Each module follows hexagonal architecture:

- **domain/** - Entities, value objects, repository port interfaces
- **application/** - Use cases, commands, services orchestrating domain logic
- **infrastructure/** - Adapter implementations:
  - `adapter-postgresql/` - Hibernate Panache + Flyway migrations
  - `adapter-rest/` - REST controllers (OpenAPI-generated interfaces)
  - `adapter-keycloak/` - Keycloak integration
  - `adapter-eventbus/` - Vert.x EventBus listeners/publishers
  - `adapter-webhook/` - Webhook handlers
- **bootstrap/** - Module startup and CDI configuration

### Key Patterns

- **Repository Pattern**: Domain defines interfaces (`UserRepository`), infrastructure implements them
- **Event-Driven Communication**: Modules communicate via Vert.x EventBus
  - Topics: `iam.user.registered`, `iam.webhook.keycloak`
- **OpenAPI-First**: REST APIs generated from `adapter-rest/src/main/resources/api.yml`
- **MapStruct Mappers**: Layer-to-layer object mapping

### Startup Sequence

Bootstrap classes observe `StartupEvent` with priority:
1. `IamBootstrap` (Priority 10) - Creates Keycloak realm, roles, webhooks
2. `PassengerBootstrap` (Priority 20)
3. `AdminBootstrap` (Priority 30)

## Technology Stack

- **Java 21**, **Maven**
- **PostgreSQL** with Hibernate Panache and Flyway
- **Keycloak 26** for authentication (OIDC)
- **HashiCorp Vault** for secrets
- **Vert.x EventBus** for inter-service messaging
- **OpenAPI Generator 7.8** + **MapStruct** + **Lombok**

## Database

Flyway migrations: `user/*/infrastructure/adapter-postgresql/src/main/resources/db/migration/`

Schema: `quarkus`

## Configuration

- Main config: `bootloader/src/main/resources/application.yml`
- Module configs: `user/*/infrastructure/adapter-*/src/main/resources/application.yml`

## Development Notes

- Dev services auto-start PostgreSQL, Keycloak, and Vault containers
- Docker must be running for dev services
- Swagger UI: `/q/swagger-ui` (dev mode only)
