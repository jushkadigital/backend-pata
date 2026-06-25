# DDD Refactoring Plan: Replace Users Subdomain with Organization

## Context

The current architecture groups modules under `user/` — a generic container that mixes:

| Concern | Current Location | Should Belong To |
|---------|-----------------|------------------|
| Authentication / Authorization | `user/identity/` (IAM + Keycloak) | **Identity Context** (keep) |
| Passenger profiles | `user/passenger/` | **Passenger Context** (keep) |
| Organizational hierarchy | mixed into identity models | **Organization Subdomain** (new) |
| Approval chains / routing | implicit in user models | **Organization Subdomain** (new) |
| Managerial relationships | none (anemic model) | **Organization Subdomain** (new) |
| Staff assignments | none (anemic model) | **Organization Subdomain** (new) |

The `identity` context already handles authentication, authorization, OAuth2/OIDC, JWT/session management, and Keycloak integration. However, the `UserSyncRecord` model still carries organizational connotations (`userType`, generic `roles` used for routing decisions).

**Problem**: The current model is anemic — `User` is a catch-all that fails to represent:
- Institutional hierarchy (areas, departments, offices)
- Positions and roles within the organization
- Staff assignments and temporary replacements
- Approval chains and workflow ownership
- Supervisory and managerial relationships

## Goal

Replace the anemic generic-user model with a dedicated **Organization Supporting Subdomain**.

The new model must explicitly represent:

- Institutional hierarchy (areas/departments/offices)
- Positions/cargos with levels
- Staff members and their assignments
- Supervisors and managers
- Temporary replacements
- Workflow ownership
- Approval hierarchy
- Routing destinations

## Strategic Design

### Bounded Contexts

```
┌────────────────────────────────────────────────────────────┐
│                    IDENTITY (Generic Subdomain)              │
│                                                             │
│  Responsible for:                                           │
│  • Authentication & Authorization                           │
│  • OAuth2 / OIDC / JWT                                     │
│  • Keycloak integration (realms, clients, users, roles)     │
│  • User identity synchronization                            │
│                                                             │
│  NOT responsible for:                                       │
│  • Office hierarchy ✘                                       │
│  • Workflow routing ✘                                       │
│  • Manager relationships ✘                                  │
│  • Business approvals ✘                                     │
│  • Organizational structure ✘                               │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                ORGANIZATION (Supporting Subdomain)           │
│                                                             │
│  Responsible for:                                           │
│  • Organizational hierarchy (areas, departments)            │
│  • Position/cargo definitions and levels                    │
│  • Staff members and their profiles                         │
│  • Staff assignments to positions/areas                     │
│  • Temporary replacements                                   │
│  • Approval chains                                          │
│  • Workflow ownership                                       │
│  • Routing destinations                                     │
│  • Supervisory relationships                                │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                    PASSENGER (Core Subdomain)                │
│                                                             │
│  Responsible for:                                           │
│  • Passenger profiles                                       │
│  • Passenger-specific business logic                        │
│                                                             │
│  References: identity (who), organization (routing)         │
└────────────────────────────────────────────────────────────┘
```

### Architectural Rule

**Do NOT use Keycloak roles/groups to model:**
- Offices or departments
- Managerial hierarchy
- Workflow approvals
- Institutional routing

These belong exclusively to the **Organization Subdomain**. Keycloak is for **identity and access control only**.

---

## Target Module Structure

```
organization/
├── pom.xml                         # Parent POM
├── domain/
│   ├── pom.xml
│   └── src/main/java/.../organization/domain/
│       ├── area/
│       │   ├── Area.java
│       │   ├── AreaId.java
│       │   └── AreaRepository.java
│       ├── position/
│       │   ├── Position.java
│       │   ├── PositionId.java
│       │   └── PositionRepository.java
│       ├── staff/
│       │   ├── StaffMember.java
│       │   ├── StaffId.java
│       │   ├── StaffStatus.java
│       │   └── StaffRepository.java
│       ├── assignment/
│       │   ├── Assignment.java
│       │   ├── AssignmentId.java
│       │   └── AssignmentRepository.java
│       ├── replacement/
│       │   ├── Replacement.java
│       │   └── ReplacementRepository.java
│       ├── policy/
│       │   └── OrganizationalPolicy.java
│       └── event/
│           ├── StaffAssignedToArea.java
│           ├── ManagerAssigned.java
│           ├── PositionChanged.java
│           ├── ReplacementAssigned.java
│           └── AreaCreated.java
│
├── application/
│   ├── pom.xml
│   └── src/main/java/.../organization/application/
│       ├── service/
│       │   ├── AreaService.java
│       │   ├── PositionService.java
│       │   ├── StaffService.java
│       │   ├── AssignmentService.java
│       │   └── OrganizationalPolicyService.java
│       ├── command/
│       │   ├── CreateAreaCommand.java
│       │   ├── AssignStaffCommand.java
│       │   ├── CreatePositionCommand.java
│       │   └── ...
│       ├── dto/
│       │   ├── AreaDTO.java
│       │   ├── StaffDTO.java
│       │   ├── PositionDTO.java
│       │   └── ...
│       └── port/
│           ├── AreaRepository.java (interface)
│           ├── PositionRepository.java (interface)
│           ├── StaffRepository.java (interface)
│           └── OrganizationEventPublisher.java
│
├── infrastructure/
│   ├── pom.xml
│   └── src/main/java/.../organization/infrastructure/
│       ├── adapter-postgresql/
│       │   ├── AreaPanacheEntity.java
│       │   ├── PositionPanacheEntity.java
│       │   ├── StaffPanacheEntity.java
│       │   ├── AssignmentPanacheEntity.java
│       │   ├── ReplacementPanacheEntity.java
│       │   ├── AreaPanacheRepository.java
│       │   ├── PositionPanacheRepository.java
│       │   ├── StaffPanacheRepository.java
│       │   └── db/migration/
│       │       └── V{next}__create_organization_tables.sql
│       ├── adapter-rest/
│       │   ├── api.yml                    # OpenAPI spec
│       │   ├── AreaController.java
│       │   ├── PositionController.java
│       │   └── StaffController.java
│       └── adapter-eventbus/
│           ├── OrganizationEventPublisher.java
│           └── OrganizationEventConsumer.java
│
└── bootstrap/
    ├── pom.xml
    └── src/main/java/.../organization/bootstrap/
        ├── OrganizationBootstrap.java
        └── OrganizationCdiConfig.java
```

---

## Domain Model

### Aggregate: Area

Represents an organizational unit (department, division, office, area).

```java
Area {
  - areaId: AreaId
  - name: String
  - code: String              // Business code (e.g., "DEPT-IT")
  - parentAreaId: AreaId      // null if root
  - level: int                // Hierarchy depth (0 = root)
  - active: boolean
  - createdAt: Instant
  - updatedAt: Instant
}

// Business rules:
// - Root areas have parentAreaId = null
// - An area cannot be deactivated if it has active staff assigned
// - Area hierarchy cannot exceed configured max depth (OrganizationalPolicy)
// - Area code must be unique
```

### Aggregate: Position

Represents a job position/cargo within an area.

```java
Position {
  - positionId: PositionId
  - name: String                  // e.g., "Department Head"
  - code: String                  // Business code (e.g., "POS-DH")
  - level: int                    // Hierarchy level for approval routing
  - areaId: AreaId
  - active: boolean
  - requiresSupervisor: boolean   // Does this position require a supervisor?
  - maxAssignments: int           // Max number of staff in this position (0 = unlimited)
  - createdAt: Instant
  - updatedAt: Instant
}

// Business rules:
// - Position belongs to exactly one Area
// - Position level determines approval chain ordering
// - A position cannot be deleted if it has active assignments
```

### Aggregate: StaffMember

Represents an individual within the organizational hierarchy.

```java
StaffMember {
  - staffId: StaffId
  - identityId: String            // Reference to identity user (Keycloak ID)
  - fullName: String
  - email: String
  - currentAreaId: AreaId         // Current primary area
  - currentPositionId: PositionId // Current primary position
  - supervisorId: StaffId         // Direct supervisor (null if top-level)
  - status: StaffStatus           // ACTIVE, INACTIVE, ON_LEAVE, SUSPENDED
  - joinedAt: Instant
  - leftAt: Instant               // null if active
  - createdAt: Instant
  - updatedAt: Instant
}

StaffStatus: ACTIVE | INACTIVE | ON_LEAVE | SUSPENDED

// Business rules:
// - A StaffMember must have a valid identity (integrity with identity context)
// - Supervisor must belong to same or parent area (configurable via policy)
// - StaffMember status changes must be domain events
// - An identityId can be linked to at most one active StaffMember
```

### Aggregate: Assignment

Tracks staff assignments to positions over time (supports history and temporary assignments).

```java
Assignment {
  - assignmentId: AssignmentId
  - staffId: StaffId
  - positionId: PositionId
  - areaId: AreaId
  - startDate: LocalDate
  - endDate: LocalDate          // null = indefinite
  - temporary: boolean          // true = temporary assignment
  - replacementForId: StaffId   // Who this assignment replaces (null if not a replacement)
  - active: boolean
  - reason: String              // Assignment reason (e.g., "MATERNITY_LEAVE", "NEW_HIRE")
  - createdAt: Instant
  - updatedAt: Instant
}

// Business rules:
// - A StaffMember can have multiple active assignments, but only one primary
// - Temporary assignments must have an endDate
// - Temporary assignments cannot exceed OrganizationalPolicy.maxTemporaryDays
// - An assignment cannot overlap another active assignment for the same staff+position
```

### Value Object: Replacement

Tracks temporary replacements explicitly.

```java
Replacement {
  - replacementId: ReplacementId
  - originalStaffId: StaffId       // The person being replaced
  - replacementStaffId: StaffId    // The person filling in
  - positionId: PositionId         // Position being covered
  - startDate: LocalDate
  - endDate: LocalDate
  - reason: ReplacementReason      // LEAVE, TRAINING, SECONDMENT, etc.
  - approvedBy: StaffId            // Who approved the replacement
  - status: ReplacementStatus      // PENDING, ACTIVE, COMPLETED, CANCELLED
  - createdAt: Instant
  - updatedAt: Instant
}

ReplacementReason: LEAVE | TRAINING | SECONDMENT | TEMPORARY_TRANSFER | OTHER
ReplacementStatus: PENDING | ACTIVE | COMPLETED | CANCELLED

// Business rules:
// - A staff member cannot be replaced when they are already a replacement for someone else
// - Replacement must be approved by a superior in the approval chain
// - Active replacements create implicit temporary Assignment records
```

### Aggregate: OrganizationalPolicy

Configuration rules that govern organizational behavior.

```java
OrganizationalPolicy {
  - maxHierarchyDepth: int           // Max area nesting depth
  - maxTemporaryDays: int            // Max days for temporary assignments
  - requireSupervisorForPositions: boolean
  - allowMultipleAssignments: boolean
  - supervisorMustBeParentArea: boolean
  - defaultMaxRetries: int
  - createdAt: Instant
  - updatedAt: Instant
}
```

---

## Current State → Target State

| Aspect | Current | Target |
|--------|---------|--------|
| Directory structure | `user/identity/`, `user/passenger/`, `user/shared/` | Keep existing + add `organization/` top-level |
| User model | `UserSyncRecord` (anemic, identity + organizational mixed) | `StaffMember` (organizational), `UserRecord` (identity only) |
| Hierarchy | None (not modeled) | `Area` with `parentAreaId` for tree structure |
| Positions | None | `Position` with level, area, and assignment rules |
| Assignments | None | `Assignment` with temporal tracking |
| Replacements | None | `Replacement` with approval flow |
| Approval chains | Implicit / not modeled | Position levels + supervisor relationships |
| Workflow routing | `User`-based (if present) | `Area` + `Position` + `StaffMember` based |
| Organizational rules | Hardcoded (if any) | `OrganizationalPolicy` aggregate |
| Sponsorship | `UserSyncRecord.userType` | Removed from identity — handled by organization |
| Events | Mixed identity + routing events | `identity.*` for auth, `organization.*` for org |

---

## Event Model

### Organization Domain Events (internal to organization context)

| Event | When Published | Payload |
|-------|---------------|---------|
| `organization.area.created.v1` | New area created | `areaId`, `name`, `parentAreaId` |
| `organization.area.deactivated.v1` | Area deactivated | `areaId`, `reason` |
| `organization.position.created.v1` | New position created | `positionId`, `name`, `areaId`, `level` |
| `organization.staff.assigned-to-area.v1` | Staff assigned to primary area | `staffId`, `areaId`, `positionId` |
| `organization.staff.position-changed.v1` | Staff position changed | `staffId`, `oldPositionId`, `newPositionId` |
| `organization.staff.supervisor-assigned.v1` | Supervisor assigned/changed | `staffId`, `supervisorId` |
| `organization.staff.status-changed.v1` | Staff status changed | `staffId`, `oldStatus`, `newStatus` |
| `organization.replacement.assigned.v1` | Replacement assigned | `originalStaffId`, `replacementStaffId`, `positionId`, `startDate`, `endDate` |
| `organization.replacement.completed.v1` | Replacement period ended | `replacementId`, `endDate` |

### Workflow Events (consumed by other contexts)

| Event | When Published | Payload |
|-------|---------------|---------|
| `organization.expediente.assigned.v1` | Expediente assigned to staff/area/position | `expedienteId`, `assignedAreaId`, `assignedPositionId`, `assignedStaffId` |
| `organization.approval.requested.v1` | Approval requested for an expediente | `expedienteId`, `approverId`, `approvalLevel` |
| `organization.expediente.derived.v1` | Expediente derived/routed | `expedienteId`, `fromAreaId`, `toAreaId`, `reason` |

---

## Execution Phases

### Phase 1: Organization Module Scaffolding (Foundation)

1. **Create module structure**
   - Create `organization/` parent POM with domain, application, infrastructure, bootstrap submodules
   - Add module references to root `pom.xml` and `bootloader/pom.xml`
   - Create domain layer packages and empty aggregates

2. **Define domain model**
   - Implement `Area`, `Position`, `StaffMember` as domain aggregates
   - Implement value objects: `AreaId`, `PositionId`, `StaffId`, `AssignmentId`, `ReplacementId`
   - Implement `StaffStatus` enum and `ReplacementReason`/`ReplacementStatus` enums
   - Implement `OrganizationalPolicy` aggregate
   - Define repository interfaces in domain layer

3. **Define domain events**
   - Create event records for each organization domain event
   - Define event publisher port interface

### Phase 2: Infrastructure Implementation

1. **PostgreSQL adapter**
   - Create Flyway migration: `V{next}__create_organization_tables.sql`
   - Implement Panache entities for all aggregates
   - Implement Panache repositories
   - Create entity mappers (MapStruct)

2. **REST adapter**
   - Create `api.yml` OpenAPI spec for organization endpoints
   - Generate REST controllers
   - Implement CRUD endpoints for Areas, Positions, StaffMembers

3. **EventBus adapter**
   - Implement `OrganizationEventPublisher` (Vert.x EventBus)
   - Implement event consumers for inter-context communication

### Phase 3: Identity Module Cleanup

1. **Remove organizational concerns from identity**
   - Strip `userType` from `UserSyncRecord` — identity only tracks auth identity
   - Keep only auth-related fields (id, email, externalId, roles, syncStatus)
   - Remove implicit routing/org semantics from identity events

2. **Update identity events**
   - Ensure identity events no longer carry organizational payloads
   - Identity publishes `identity.user.*` events only

### Phase 4: Workflow Integration

1. **Replace user-based routing with organization-based routing**
   - Update `Expediente.assignedUserId` → `Expediente.assignedAreaId`, `Expediente.assignedPositionId`, `Expediente.assignedStaffId`
   - Update workflow services to query organization for routing decisions

2. **Implement approval chain resolution**
   - Use `Position.level` + supervisor relationships to determine approval chain
   - Implement approval routing service in organization application layer

### Phase 5: Bootstrap & Configuration

1. **Create `OrganizationBootstrap`**
   - Observe `StartupEvent` with appropriate priority
   - Initialize default organizational policies
   - Register EventBus consumers

2. **Wire modules in bootloader**
   - Add organization module to bootloader dependencies
   - Configure organization in `application.yml`
   - Update `CLAUDE.md` and `AGENTE.md` documentation

### Phase 6: Verification & Documentation

1. **Build verification**
   - `mvn clean compile` passes
   - `mvn test` passes (all tests)

2. **Documentation update**
   - Update `AGENTE.md` with new organization module structure
   - Update `CLAUDE.md` with new module references
   - Generate architecture diagrams

3. **Verify no residual generic-user patterns**
   - All models use organizational concepts (Area, Position, StaffMember)
   - No workflow routing decisions based on `userType` or identity roles
   - No Keycloak groups used for organizational hierarchy

---

## Execution Order (Dependencies)

```
Phase 1 (Scaffolding + Domain Model) ← Foundation, must happen first
   ↓
Phase 2 (Infrastructure: DB, REST, Events) ← Depends on Phase 1 domain model
   ↓
Phase 3 (Identity Cleanup) ← Can overlap with Phase 2, but needs Phase 1 event definitions
   ↓
Phase 4 (Workflow Integration) ← Depends on Phase 1 + 2 (needs running organization service)
   ↓
Phase 5 (Bootstrap & Config) ← Depends on Phase 2 (infrastructure ready)
   ↓
Phase 6 (Verification & Docs) ← Final
```

## Parallelization Opportunities

- **Phase 1 sub-tasks**: Domain model definition, repository interfaces, and event definitions can be done in parallel
- **Phase 2 sub-tasks**: PostgreSQL, REST, and EventBus adapters can be implemented in parallel
- **Phase 3** can start once Phase 1 event definitions are stable (overlap with Phase 2)
- **Phase 5** can start once Phase 2 infrastructure is complete

## Risk Mitigations

1. **Identity coupling**: Identity module currently has implicit organizational semantics — must ensure clean separation before removing fields
2. **Flyway migrations**: All new organization tables should be created in a single migration; identity table changes should be separate
3. **Backward compatibility**: Workflow integration (Phase 4) must handle both old user-based and new organization-based routing during transition
4. **Data integrity**: StaffMember ↔ identityId reference must be consistent — consider eventual consistency with identity events
5. **Approval chain complexity**: Start with a simple linear approval chain model, then iterate
6. **Event versioning**: New organization events should use `v1` from the start; old events should be deprecated, not removed, during transition

## References

- **Existing plan**: `.omo/plans/ddd-eda-refactor.md` (admin domain elimination, precedent for this refactoring)
- **Architecture doc**: `AGENTE.md` (current project structure documentation)
- **Identity module**: `user/identity/` (to be cleaned of organizational concerns)
- **Passenger module**: `user/passenger/` (consumer of organization events)
