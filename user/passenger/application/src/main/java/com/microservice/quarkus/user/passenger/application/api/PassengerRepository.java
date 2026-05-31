package com.microservice.quarkus.user.passenger.application.api;

import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import java.util.Optional;

/**
 * Repository port for Passenger persistence.
 * Application layer defines this; infrastructure layer implements it.
 */
public interface PassengerRepository {
    void save(Passenger passenger);

    Optional<Passenger> findByExternalId(String externalId);

    Optional<Passenger> findByEmail(EmailAddress email);

    void deleteByExternalId(String externalId);
}
