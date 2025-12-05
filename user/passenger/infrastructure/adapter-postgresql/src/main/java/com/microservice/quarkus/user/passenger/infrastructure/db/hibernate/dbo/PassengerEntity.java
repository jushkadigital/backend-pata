package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.converter.EmailAddressConverter;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerType;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerStatus;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.converter.PassengerIdConverter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "passengers", schema = "quarkus")
@Getter
@Setter
public class PassengerEntity {
  @Id
  private String id;
  @Convert(converter = EmailAddressConverter.class)
  @Column(name = "email", nullable = false)
  private EmailAddress email;

  @Enumerated(EnumType.STRING)
  @Column(name = "passenger_type", nullable = false)
  private PassengerType type;

  @Column(name = "external_id", nullable = false)
  private String externalId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PassengerStatus status;

  @Column(name = "first_names")
  private String firstNames;

  @Column(name = "last_names")
  private String lastNames;

  @Column(name = "dni")
  private String dni;

  @Column(name = "phone")
  private String phone;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

}
