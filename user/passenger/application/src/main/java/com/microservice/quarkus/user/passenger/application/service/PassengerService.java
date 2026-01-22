package com.microservice.quarkus.user.passenger.application.service;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerType;
import com.microservice.quarkus.user.passenger.domain.ports.out.PassengerRepository;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.shared.domain.outbox.EventScope;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.passenger.domain.shared.DomainEvent;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;

import io.vertx.core.json.JsonObject;
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
  OutboxEventRepository outboxEventRepository;

  private PassengerRepository userRepository;

  @Inject
  public PassengerService(PassengerRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public String register(CreatePassengerCommand cmd) {

    // Crear y guardar el agregado
    Passenger passenger = Passenger.createNew(cmd.externalId(), cmd.email(), cmd.subType());
    userRepository.save(passenger);

    // Guardar eventos de dominio en la tabla outbox compartida (MISMA TRANSACCIÓN)
    passenger.domainEvents().forEach(domainEvent -> {
      OutboxEvent outboxEvent = OutboxEvent.create(
          "Passenger", // subdomain
          "Passenger", // aggregateType
          passenger.getId().value(),
          domainEvent.getClass().getSimpleName(),
          JsonObject.mapFrom(domainEvent).encode(),
          EventScope.BOTH,
          domainEvent.occurredOn());

      outboxEventRepository.save(outboxEvent);
      System.out.println("📦 Outbox: Evento " + domainEvent.getClass().getSimpleName() +
          " guardado para subdomain Passenger: " + cmd.email());
    });

    // Limpiar eventos del agregado después de guardarlos en outbox
    passenger.clearDomainEvents();

    return passenger.getId().value();
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
