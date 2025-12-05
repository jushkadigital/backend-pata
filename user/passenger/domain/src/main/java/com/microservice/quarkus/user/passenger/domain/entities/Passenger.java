package com.microservice.quarkus.user.passenger.domain.entities;

import java.time.Instant;
import java.util.List;

import com.microservice.quarkus.user.passenger.domain.shared.Entity;
import com.microservice.quarkus.user.passenger.domain.shared.RootAggregate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Passenger extends RootAggregate implements Entity<Passenger> {
  private PassengerId id;
  private PassengerType type;
  private EmailAddress email;
  private String externalId;
  private Instant createdAt;
  private Instant updatedAt;
  private PassengerStatus status;

  private String firstNames;
  private String lastNames;
  private String dni;
  private String phone;

  public static Passenger createNew(String externalId, String email, String type) {
    Instant now = Instant.now();
    return Passenger.builder()
        .id(PassengerId.random())
        .status(PassengerStatus.INCOMPLETE_PROFILE)
        .externalId(externalId)
        .email(new EmailAddress(email))
        .type(PassengerType.valueOf(type.toUpperCase()))
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  public void completeProfile(String firstNames, String lastNames, String dni, String phone) {
    if (this.status == PassengerStatus.ACTIVE) {
      throw new IllegalArgumentException("YA ESTA COMPLETO");
    }
    this.firstNames = firstNames;
    this.lastNames = lastNames;
    this.dni = dni;
    this.phone = phone;
    this.status = PassengerStatus.ACTIVE;
  }

  @Override
  public boolean sameIdentityAs(Passenger other) {
    return other != null && this.id.equals(other.getId());
  }
}
