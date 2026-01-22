package com.microservice.quarkus.user.admin.application.service;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.AdminType;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.user.admin.domain.ports.out.AdminRepository;
import com.microservice.quarkus.user.shared.domain.outbox.EventScope;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.admin.domain.shared.DomainEvent;
import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;

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
public class AdminService {

  @Inject
  OutboxEventRepository outboxEventRepository;

  private AdminRepository userRepository;

  @Inject
  public AdminService(AdminRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public String register(CreateAdminCommand cmd) {

    // Crear y guardar el agregado
    Admin admin = Admin.createNew(cmd.externalId(), cmd.email(), cmd.type());
    userRepository.save(admin);

    // Guardar eventos de dominio en la tabla outbox compartida (MISMA TRANSACCIÓN)
    admin.domainEvents().forEach(domainEvent -> {
      OutboxEvent outboxEvent = OutboxEvent.create(
          "Admin", // subdomain
          "Admin", // aggregateType
          admin.getId().value(),
          domainEvent.getClass().getSimpleName(),
          JsonObject.mapFrom(domainEvent).encode(),
          EventScope.BOTH,
          domainEvent.occurredOn());

      outboxEventRepository.save(outboxEvent);
      System.out.println("📦 Outbox: Evento " + domainEvent.getClass().getSimpleName() +
          " guardado para subdomain Admin: " + cmd.email());
    });

    // Limpiar eventos del agregado después de guardarlos en outbox
    admin.clearDomainEvents();

    return admin.getId().value();
  }

}
