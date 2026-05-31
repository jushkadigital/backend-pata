package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.domain.PassengerId;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.passenger.domain.PassengerType;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Maps between domain Passenger and infrastructure PassengerEntity.
 * No MapStruct — simple enough for manual mapping.
 */
@ApplicationScoped
public class PassengerEntityMapper {

    public PassengerEntity toEntity(Passenger passenger) {
        PassengerEntity entity = new PassengerEntity();
        entity.setId(passenger.getId().value());
        entity.setEmail(passenger.getEmail());
        entity.setExternalId(passenger.getExternalId());
        entity.setType(passenger.getType());
        entity.setStatus(passenger.getStatus());
        entity.setFirstNames(passenger.getFirstNames());
        entity.setLastNames(passenger.getLastNames());
        entity.setDni(passenger.getDni());
        entity.setPhone(passenger.getPhone());
        entity.setCreatedAt(passenger.getCreatedAt());
        entity.setUpdatedAt(passenger.getUpdatedAt());
        return entity;
    }

    public Passenger toDomain(PassengerEntity entity) {
        return Passenger.reconstruct(
            PassengerId.of(entity.getId()),
            entity.getEmail(),
            entity.getExternalId(),
            entity.getType(),
            entity.getStatus(),
            entity.getFirstNames(),
            entity.getLastNames(),
            entity.getDni(),
            entity.getPhone(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public void updateEntity(PassengerEntity entity, Passenger passenger) {
        entity.setEmail(passenger.getEmail());
        entity.setExternalId(passenger.getExternalId());
        entity.setType(passenger.getType());
        entity.setStatus(passenger.getStatus());
        entity.setFirstNames(passenger.getFirstNames());
        entity.setLastNames(passenger.getLastNames());
        entity.setDni(passenger.getDni());
        entity.setPhone(passenger.getPhone());
        entity.setUpdatedAt(passenger.getUpdatedAt());
    }
}
