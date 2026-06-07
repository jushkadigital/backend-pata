package com.microservice.quarkus.user.passenger.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.passenger.domain.PassengerType;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @Mock
    PassengerRepository repository;

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    TraceContextProvider traceContextProvider;

    @Mock
    PassengerMetrics passengerMetrics;

    private PassengerService createService() {
        lenient().when(traceContextProvider.current()).thenReturn(EventMetadata.empty());
        return new PassengerService(repository, outboxEventRepository, traceContextProvider, passengerMetrics);
    }

    @Test
    void register_shouldCreatePassengerWithIncompleteStatus() {
        PassengerService service = createService();
        CreatePassengerCommand cmd = new CreatePassengerCommand(
                "ext-123", "john@example.com", "STANDARD");

        AtomicReference<Passenger> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).save(any(Passenger.class));

        String id = service.register(cmd);

        assertNotNull(id);
        Passenger passenger = captured.get();
        assertNotNull(passenger);
        assertEquals("ext-123", passenger.getExternalId());
        assertEquals(new EmailAddress("john@example.com"), passenger.getEmail());
        assertEquals(PassengerType.STANDARD, passenger.getType());
        assertEquals(PassengerStatus.INCOMPLETE_PROFILE, passenger.getStatus());
        assertNotNull(passenger.getCreatedAt());
        assertNotNull(passenger.getUpdatedAt());
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void register_shouldCreateOutboxEvent() {
        PassengerService service = createService();
        CreatePassengerCommand cmd = new CreatePassengerCommand(
                "ext-456", "jane@example.com", "PREMIUM");

        service.register(cmd);

        verify(outboxEventRepository, times(2)).save(argThat(event ->
                ("passenger.created.v1".equals(event.getEventType())
                        || "notification.passenger.created.v1".equals(event.getEventType()))
                        && "Passenger".equals(event.getAggregateType())
                        && !event.getPublished()));
    }

    @Test
    void register_shouldSetPremiumType() {
        PassengerService service = createService();
        CreatePassengerCommand cmd = new CreatePassengerCommand(
                "ext-789", "premium@example.com", "PREMIUM");

        AtomicReference<Passenger> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).save(any(Passenger.class));

        service.register(cmd);

        assertEquals(PassengerType.PREMIUM, captured.get().getType());
    }

    @Test
    void register_shouldSetBasicType() {
        PassengerService service = createService();
        CreatePassengerCommand cmd = new CreatePassengerCommand(
                "ext-101", "basic@example.com", "BASIC");

        AtomicReference<Passenger> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(repository).save(any(Passenger.class));

        service.register(cmd);

        assertEquals(PassengerType.BASIC, captured.get().getType());
    }

    @Test
    void complete_shouldUpdatePassengerToActiveStatus() {
        PassengerService service = createService();
        Passenger existing = Passenger.reconstruct(
                com.microservice.quarkus.user.passenger.domain.PassengerId.of("pass-123"),
                new EmailAddress("john@example.com"),
                "ext-123",
                PassengerType.STANDARD,
                PassengerStatus.INCOMPLETE_PROFILE,
                null, null, null, null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(repository.findByExternalId("ext-123")).thenReturn(Optional.of(existing));

        String id = service.complete("ext-123",
                new CompletePassengerCommand("John", "Doe", "12345678", "+1234567890"));

        assertEquals("pass-123", id);
        assertEquals(PassengerStatus.ACTIVE, existing.getStatus());
        assertEquals("John", existing.getFirstNames());
        assertEquals("Doe", existing.getLastNames());
        assertEquals("12345678", existing.getDni());
        assertEquals("+1234567890", existing.getPhone());
    }

    @Test
    void complete_shouldThrowWhenPassengerNotFound() {
        PassengerService service = createService();
        when(repository.findByExternalId("nonexistent")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.complete("nonexistent",
                        new CompletePassengerCommand("John", "Doe", "12345678", "+1234567890")));

        assertTrue(exception.getMessage().contains("Passenger not found"));
    }

    @Test
    void complete_shouldThrowWhenProfileAlreadyComplete() {
        PassengerService service = createService();
        Passenger existing = Passenger.reconstruct(
                com.microservice.quarkus.user.passenger.domain.PassengerId.of("pass-456"),
                new EmailAddress("jane@example.com"),
                "ext-456",
                PassengerType.STANDARD,
                PassengerStatus.ACTIVE,
                "Jane", "Doe", "87654321", "+1111111111",
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(repository.findByExternalId("ext-456")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.complete("ext-456",
                        new CompletePassengerCommand("Jane", "Doe", "87654321", "+0987654321")));

        assertTrue(exception.getMessage().contains("Profile already complete"));
    }

    @Test
    void complete_shouldUpdateTimestamp() {
        PassengerService service = createService();
        Passenger existing = Passenger.reconstruct(
                com.microservice.quarkus.user.passenger.domain.PassengerId.of("pass-789"),
                new EmailAddress("maria@example.com"),
                "ext-789",
                PassengerType.STANDARD,
                PassengerStatus.INCOMPLETE_PROFILE,
                null, null, null, null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(repository.findByExternalId("ext-789")).thenReturn(Optional.of(existing));

        service.complete("ext-789",
                new CompletePassengerCommand("Maria", "Garcia", "11223344", "+51112233445"));

        assertNotNull(existing.getUpdatedAt());
    }
}
