# DDD + EDA Refactoring Plan: Eliminate Admin Domain

## Context
- Admin is NOT a business domain — it's an authorization concern
- Keycloak is source of truth for roles
- No production data to preserve (DROP puro)
- No dual-publishing needed (clean break)
- Events belong to the domain that owns the fact

## Current State → Target State

| Aspect | Current | Target |
|--------|---------|--------|
| Bounded Contexts | identity, admin, passenger | identity, passenger |
| Admin | Separate entity/table/module | Role within Identity |
| Role source | App DB (type field) | Keycloak (JWT) |
| Events | admin.registered.v1, identity.user.registered.v1 | identity.user.created.v1, identity.user.role-assigned.v1 |
| Exchanges | user.identity, user.admin, user.passenger, user.notification | identity.events, passenger.events |
| User model | UserSyncRecord (type: ADMIN/PASSENGER) | UserRecord (roles: Set<String>) |

---

## Phase 1: Identity Model Evolution (Foundation)

### 1.1 Evolve UserSyncRecord → UserRecord
- Rename `UserSyncRecord` → `UserRecord`
- Replace `type` (UserType enum: ADMIN/PASSENGER) with `roles` (Set<String>)
- Add `disabled` boolean field
- Update `IdentitySyncRepository` interface
- Update `IdentityPersistenceService`

### 1.2 New Identity Events
- `identity.user.created.v1` (replaces `identity.user.registered.v1`)
- `identity.user.role-assigned.v1` (NEW)
- `identity.user.role-removed.v1` (NEW — placeholder for future)
- `identity.user.disabled.v1` (NEW — placeholder)
- `identity.user.deleted.v1` (replaces `identity.user.deleted.v1`)
- Delete `admin.registered.v1` and `notification.admin.registered.v1`

### 1.3 Keycloak Group Assignment Moves to Identity
- Move ADMIN group mapping (SUPER_ADMIN→admin/super-admin, etc.) from deleted admin module to identity
- Identity module handles ALL Keycloak group assignments based on roles
- When user is created with roles=[ADMIN, PASSENGER], identity assigns both admin AND passenger groups

### 1.4 Identity Events Payload Changes
- `UserCreatedEvent` includes `roles` (Set<String>) instead of `type` (single value)
- `RoleAssignedEvent` includes `role`, `userId`, `assignedBy`
- Remove `subType` concept — replaced by roles

---

## Phase 2: Admin Module Deletion

### 2.1 Delete admin module directories
- Delete `user/admin/` entirely (all submodules)
- Remove from parent POMs: `user/admin/pom.xml`, `user/pom.xml`, `bootloader/pom.xml`

### 2.2 Remove admin from shared infrastructure
- Remove `admin.registered.v1` and `notification.admin.registered.v1` from `OutboxEventPublisher.getTopicForEvent()`
- Remove `admin-events-out` emitter from `RabbitMqBroker`
- Remove `user.admin` exchange from `RabbitMqTopologyDeclarator`
- Remove admin channel from `application.yml`

### 2.3 Database migrations
- V15: Drop `quarkus.admin` table (if exists — check actual table name from migrations)
- V16: Add `roles` column to user sync table, migrate `type` → `roles`

---

## Phase 3: Passenger Module Updates

### 3.1 Rename passenger events
- `passenger.registered.v1` → `passenger.created.v1`
- `PassengerRegisteredEvent` → `PassengerCreatedEvent`
- Update `PassengerService` to use `PassengerCreatedEvent.v1()`
- Update `notification.passenger.registered.v1` → `notification.passenger.created.v1`

### 3.2 Update passenger consumers
- Listen to `identity.user.created` instead of `identity.user.registered`
- Update `UserRegisteredEventDTO` to use `roles` (Set<String>) instead of `type`/`subType`
- Passenger creates profile when user has PASSENGER role (not `type=PASSENGER`)
- Remove `PassengerRepository` dependency from listener (idempotency handled by IdempotencyStore)

### 3.3 Remove passenger-specific Keycloak group assignment from listener
- Identity module handles ALL group assignments now
- Passenger listener only creates passenger profile + emits passenger event

---

## Phase 4: RabbitMQ Topology Refactoring

### 4.1 Exchange renames
- `user.identity` → `identity.events`
- `user.passenger` → `passenger.events`
- Delete `user.admin` (exchange, queues, DLQ, retry, DLX — all)
- `user.notification` → keep as `notification.events` (or fold into producer exchanges)

### 4.2 Queue naming convention
- Consumer-owned queues: `{consumer}.{producer}` format
- Examples: `passenger.identity.sync`, `notification.identity`

### 4.3 Update RabbitMqBroker
- Remove `admin-events-out` emitter
- Rename emitters: `identity-events-out`, `passenger-events-out`, `notification-events-out`
- Update `getEmitterForEventType()` to use new prefixes

### 4.4 Update OutboxEventPublisher
- Update `getTopicForEvent()` and `getExchangeForEventType()`
- Add domain routing: `identity.*` → identity.events, `passenger.*` → passenger.events
- Remove admin routing

### 4.5 Update application.yml (shared/adapter-eventbus)
- Replace channel names
- Update exchange names

---

## Phase 5: Verification & Cleanup

### 5.1 Build verification
- `mvn clean compile` passes
- `mvn test` passes (all tests, including renamed events)

### 5.2 Remove dead code
- Delete `AdminRegisteredEvent`, `AdminService`, `AdminEntity`, `AdminPanacheRepository`, `AdminMetrics`
- Delete `UserRegisteredListener` (admin module version)
- Delete `UserDeletedListener` (admin module version)
- Delete admin `EventContractTest`, admin listener tests
- Delete `AdminEventCatalog` (already removed)

### 5.3 Verify no references remain
- `grep -r "admin.registered" --include="*.java"`
- `grep -r "AdminEntity" --include="*.java"`
- `grep -r "AdminService" --include="*.java"`
- `grep -r "user.admin" --include="*.java" --include="*.yml"`

---

## Execution Order (Dependencies)

```
Phase 1 (Identity evolution) ← must happen FIRST (other modules depend on new events)
   ↓
Phase 2 (Admin deletion) ← can start after Phase 1 events are defined
   ↓
Phase 3 (Passenger updates) ← depends on Phase 1 new events
   ↓
Phase 4 (RabbitMQ topology) ← depends on Phase 2 + 3 (need new exchange names)
   ↓
Phase 5 (Verification) ← final
```

## Parallelization Opportunities

- Phase 1 and Phase 2 can partially overlap (delete admin code while building identity events)
- Phase 3 depends on Phase 1 new event DTOs being ready
- Phase 4 depends on Phase 2 and 3 for correct exchange/queue names

## Risk Mitigations

1. **Keycloak group assignment**: Identity must handle admin group assignment BEFORE deleting admin module
2. **Passenger consumer**: Must subscribe to new event name (`identity.user.created`) BEFORE old one (`identity.user.registered`) stops being published
3. **Flyway**: All migrations must be additive first (add columns), then drop tables in separate migration
4. **Tests**: Run full test suite after each phase, not just at the end
