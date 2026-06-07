package com.microservice.quarkus.user.passenger.domain;

import java.time.Instant;

import com.microservice.quarkus.user.shared.domain.EmailAddress;
import com.microservice.quarkus.user.shared.domain.RootAggregate;

/**
 * Passenger aggregate root — pure domain model, no framework deps.
 * Encapsulates business rules for passenger profile lifecycle.
 */
public class Passenger extends RootAggregate {

    private PassengerId id;
    private EmailAddress email;
    private String externalId;
    private PassengerType type;
    private PassengerStatus status;
    private String firstNames;
    private String lastNames;
    private String dni;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;

    private Passenger() {}

    /**
     * Factory method — creates a new passenger with INCOMPLETE_PROFILE status.
     */
    public static Passenger createNew(String email, String externalId, String passengerType) {
        Instant now = Instant.now();
        Passenger passenger = new Passenger();
        passenger.id = PassengerId.random();
        passenger.email = new EmailAddress(email);
        passenger.externalId = externalId;
        passenger.type = PassengerType.valueOf(passengerType.toUpperCase());
        passenger.status = PassengerStatus.INCOMPLETE_PROFILE;
        passenger.createdAt = now;
        passenger.updatedAt = now;
        return passenger;
    }

    /**
     * Reconstruct a passenger from persistence — used by repository mapper.
     */
    public static Passenger reconstruct(PassengerId id, EmailAddress email, String externalId,
                                        PassengerType type, PassengerStatus status,
                                        String firstNames, String lastNames, String dni, String phone,
                                        Instant createdAt, Instant updatedAt) {
        Passenger passenger = new Passenger();
        passenger.id = id;
        passenger.email = email;
        passenger.externalId = externalId;
        passenger.type = type;
        passenger.status = status;
        passenger.firstNames = firstNames;
        passenger.lastNames = lastNames;
        passenger.dni = dni;
        passenger.phone = phone;
        passenger.createdAt = createdAt;
        passenger.updatedAt = updatedAt;
        return passenger;
    }

    /**
     * Business rule: complete the passenger profile.
     * Only allowed if status is INCOMPLETE_PROFILE.
     * Transitions status to ACTIVE.
     */
    public void completeProfile(String firstNames, String lastNames, String dni, String phone) {
        if (this.status == PassengerStatus.ACTIVE) {
            throw new IllegalArgumentException("Profile already complete");
        }
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.dni = dni;
        this.phone = phone;
        this.status = PassengerStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isProfileComplete() {
        return this.status == PassengerStatus.ACTIVE;
    }

    // Getters — no setters, mutations only through business methods
    public PassengerId getId() { return id; }
    public EmailAddress getEmail() { return email; }
    public String getExternalId() { return externalId; }
    public PassengerType getType() { return type; }
    public PassengerStatus getStatus() { return status; }
    public String getFirstNames() { return firstNames; }
    public String getLastNames() { return lastNames; }
    public String getDni() { return dni; }
    public String getPhone() { return phone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
