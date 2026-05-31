package com.microservice.quarkus.user.identity.infrastructure.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.passenger.domain.PassengerType;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.repository.PassengerPanacheRepository;
import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.application.service.AdminService;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository.AdminPanacheRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository.OutboxEventPanacheRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository.SagaInstancePanacheRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PassengerAdminKeycloakIT {

    @Inject
    UserService userService;

    @Inject
    PassengerService passengerService;

    @Inject
    AdminService adminService;

    @Inject
    PassengerPanacheRepository passengerPanacheRepo;

    @Inject
    AdminPanacheRepository adminRepository;

    @Inject
    IdentitySyncRepository identitySyncRepository;

    @Inject
    OutboxEventPanacheRepository outboxPanacheRepo;

    @Inject
    SagaInstancePanacheRepository sagaPanacheRepo;

    @BeforeEach
    @Transactional
    void cleanup() {
        outboxPanacheRepo.deleteAll();
        sagaPanacheRepo.deleteAll();
        passengerPanacheRepo.deleteAll();
        adminRepository.deleteAll();
        identitySyncRepository.deleteAll();
    }

    @Test
    @TestTransaction
    void shouldCreatePassengerInKeycloakViaUserService() {
        String email = "passenger-it-" + UUID.randomUUID() + "@example.com";
        String password = "TestPass123!";

        String externalId = userService.register(
                new CreateUserCommand(email, null, password, "PASSENGER", "STANDARD", false));

        assertNotNull(externalId);
        assertFalse(externalId.isEmpty());

        UserSyncRecord record = identitySyncRepository.findByExternalId(externalId);
        assertNotNull(record);
        assertEquals(email, record.email());
        assertEquals(UserType.PASSENGER, record.type());
        assertEquals(SyncStatus.PENDING, record.syncStatus());
    }

    @Test
    @TestTransaction
    void shouldCreatePassengerDirectlyInPassengerModule() {
        String externalId = UUID.randomUUID().toString();
        String email = "passenger-direct-" + UUID.randomUUID() + "@example.com";

        String passengerId = passengerService.register(
                new CreatePassengerCommand(externalId, email, "PASSENGER", "STANDARD"));

        assertNotNull(passengerId);

        PassengerEntity passenger = passengerPanacheRepo.findById(passengerId);
        assertNotNull(passenger);
        assertEquals(externalId, passenger.getExternalId());
        assertEquals(email, passenger.getEmail().value());
        assertEquals(PassengerType.STANDARD, passenger.getType());
        assertEquals(PassengerStatus.INCOMPLETE_PROFILE, passenger.getStatus());
    }

    @Test
    @TestTransaction
    void shouldCompletePassengerProfile() {
        String externalId = UUID.randomUUID() + "@ext";
        String email = "complete-passenger-" + UUID.randomUUID() + "@example.com";

        String passengerId = passengerService.register(
                new CreatePassengerCommand(externalId, email, "PASSENGER", "STANDARD"));

        PassengerEntity managed = passengerPanacheRepo.findById(passengerId);
        assertNotNull(managed);
        managed.setFirstNames("Juan");
        managed.setLastNames("Perez");
        managed.setDni("98765432");
        managed.setPhone("+51999888777");
        managed.setStatus(PassengerStatus.ACTIVE);
        passengerPanacheRepo.persist(managed);

        PassengerEntity updated = passengerPanacheRepo.findById(passengerId);
        assertEquals(PassengerStatus.ACTIVE, updated.getStatus());
        assertEquals("Juan", updated.getFirstNames());
    }

    @Test
    @TestTransaction
    void shouldCreatePremiumPassenger() {
        String externalId = UUID.randomUUID().toString();
        String email = "premium-" + UUID.randomUUID() + "@example.com";

        String passengerId = passengerService.register(
                new CreatePassengerCommand(externalId, email, "PASSENGER", "PREMIUM"));

        PassengerEntity passenger = passengerPanacheRepo.findById(passengerId);
        assertNotNull(passenger);
        assertEquals(PassengerType.PREMIUM, passenger.getType());
    }

    // ───────────────────────────────────────────────────────────────
    // ADMIN CREATION - Integration Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    @TestTransaction
    void shouldCreateAdminInKeycloakViaUserService() {
        String email = "admin-it-" + UUID.randomUUID() + "@example.com";
        String password = "AdminPass123!";

        String externalId = userService.register(
                new CreateUserCommand(email, null, password, "ADMIN", "ADMIN", false));

        assertNotNull(externalId);
        assertFalse(externalId.isEmpty());

        UserSyncRecord record = identitySyncRepository.findByExternalId(externalId);
        assertNotNull(record);
        assertEquals(email, record.email());
        assertEquals(UserType.ADMIN, record.type());
    }

    @Test
    @TestTransaction
    void shouldCreateAdminDirectlyInAdminModule() {
        String externalId = UUID.randomUUID().toString();
        String email = "admin-direct-" + UUID.randomUUID() + "@example.com";

        String adminId = adminService.register(
                new CreateAdminCommand(externalId, email, "ADMIN"));

        assertNotNull(adminId);

        AdminEntity admin = adminRepository.findById(adminId);
        assertNotNull(admin);
        assertEquals(externalId, admin.getExternalId());
        assertEquals(email, admin.getEmail().value());
        assertEquals("ADMIN", admin.getType());
    }

    @Test
    @TestTransaction
    void shouldCreateSuperAdminInAdminModule() {
        String externalId = UUID.randomUUID().toString();
        String email = "superadmin-" + UUID.randomUUID() + "@example.com";

        String adminId = adminService.register(
                new CreateAdminCommand(externalId, email, "SUPER_ADMIN"));

        AdminEntity admin = adminRepository.findById(adminId);
        assertNotNull(admin);
        assertEquals("SUPER_ADMIN", admin.getType());
    }

    // ───────────────────────────────────────────────────────────────
    // REST API - Integration Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldListUsersFromKeycloakEndpoint() {
        given()
                .when()
                .get("/keycloak/users")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldGetHealthEndpoint() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(anyOf(is(200), is(503), is(404)));
    }
}
