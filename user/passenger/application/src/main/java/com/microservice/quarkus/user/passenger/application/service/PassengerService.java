package com.microservice.quarkus.user.passenger.application.service;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerType;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PassengerService {

  @Inject
  PassengerRepositoryImpl userRepository;

  @Transactional
  public String register(CreatePassengerCommand cmd) {

    Passenger user = Passenger.createNew(cmd.externalId(), cmd.email());
    userRepository.save(user);

    return user.getId().value();
  }

  @Transactional
  public String complete(String id, CompletePassengerCommand cmd) {
    Passenger passenger = userRepository.findByExternalId(id);
    if (passenger == null) {
      throw new IllegalArgumentException("Passenger not found with externalId: " + id);
    }
    passenger.completeProfile(cmd.firstNames(), cmd.lastNames(), cmd.dni(), cmd.phone());
    userRepository.update(passenger);

    return passenger.getId().value();
  }

}
