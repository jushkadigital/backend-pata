package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.mapper.PassengerEntityMapper;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class PassengerPanacheRepository implements PanacheRepositoryBase<PassengerEntity, String>, PassengerRepository {

    @Inject
    PassengerEntityMapper mapper;

    @Override
    public void save(Passenger passenger) {
        PassengerEntity managed = find("externalId", passenger.getExternalId()).firstResult();
        if (managed != null) {
            mapper.updateEntity(managed, passenger);
        } else {
            PassengerEntity entity = mapper.toEntity(passenger);
            persist(entity);
        }
    }

    @Override
    public Optional<Passenger> findByExternalId(String externalId) {
        PassengerEntity entity = find("externalId", externalId).firstResult();
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public Optional<Passenger> findByEmail(EmailAddress email) {
        PassengerEntity entity = find("email", email).firstResult();
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public void deleteByExternalId(String externalId) {
        delete("externalId", externalId);
    }
}
