package com.microservice.quarkus.user.integration;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.domain.Passenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class TestDbHelper {

    @Inject
    PassengerRepository passengerRepository;

    @Transactional
    public Optional<Passenger> findPassengerByExternalId(String externalId) {
        return passengerRepository.findByExternalId(externalId);
    }

    @Transactional
    public void deleteTestPassengers(String... externalIds) {
        for (String id : externalIds) {
            passengerRepository.deleteByExternalId(id);
        }
    }
}
