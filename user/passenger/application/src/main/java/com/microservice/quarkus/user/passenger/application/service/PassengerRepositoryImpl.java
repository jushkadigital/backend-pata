package com.microservice.quarkus.user.passenger.application.service;

import java.util.List;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.domain.ports.out.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.api.PassengerApiService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PassengerRepositoryImpl implements PassengerApiService {

  PassengerRepository userRepository;

  public PassengerRepositoryImpl(PassengerRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Passenger findById(PassengerId id) {
    return userRepository.findById(id);
  }

  @Override
  public Passenger findByEmail(EmailAddress email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public Passenger findByExternalId(String id) {
    return userRepository.findByExternalId(id);
  }

  @Override
  public void delete(String id) {

    userRepository.delete(id);
  }

  @Override
  public List<Passenger> getAll() {
    return userRepository.getAll();
  }

  @Override
  public void save(Passenger loan) {

    userRepository.save(loan);

  }

  @Override
  public void update(Passenger loan) {

    userRepository.update(loan);
  }
}
