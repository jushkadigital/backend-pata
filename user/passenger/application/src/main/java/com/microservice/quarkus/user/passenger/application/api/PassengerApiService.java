package com.microservice.quarkus.user.passenger.application.api;

import java.util.List;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;

public interface PassengerApiService {
  public Passenger findById(PassengerId id);

  public Passenger findByEmail(EmailAddress email);

  public Passenger findByExternalId(String externalId);

  public List<Passenger> getAll();

  public void delete(String id);

  public void update(Passenger user);

  public void save(Passenger user);
}
