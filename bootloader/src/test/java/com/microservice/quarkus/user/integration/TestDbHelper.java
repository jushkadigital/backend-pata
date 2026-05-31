package com.microservice.quarkus.user.integration;

import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository.AdminPanacheRepository;
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

    @Inject
    AdminPanacheRepository adminPanacheRepository;

    @Transactional
    public Optional<Passenger> findPassengerByExternalId(String externalId) {
        return passengerRepository.findByExternalId(externalId);
    }

    @Transactional
    public AdminEntity findAdminByExternalId(String externalId) {
        return adminPanacheRepository.find("externalId", externalId).firstResult();
    }

    @Transactional
    public void deleteTestPassengers(String... externalIds) {
        for (String id : externalIds) {
            passengerRepository.deleteByExternalId(id);
        }
    }

    @Transactional
    public void deleteTestAdmins(String... externalIds) {
        for (String id : externalIds) {
            adminPanacheRepository.deleteByExternalId(id);
        }
    }
}
