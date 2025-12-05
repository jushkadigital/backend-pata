package com.microservice.quarkus.user.passenger.domain.ports.out;

import java.util.*;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;

public interface PassengerRepository {
  public Passenger findById(PassengerId id);

  public Passenger findByEmail(EmailAddress email);

  public Passenger findByExternalId(String externalId);

  public List<Passenger> getAll();

  void save(Passenger user);

  void update(Passenger user);

  void delete(String id);
}
