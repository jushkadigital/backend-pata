package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.domain.ports.out.PassengerRepository;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.exceptions.DboException;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.mapper.PassengerMapper;

@ApplicationScoped
public class PassengerDboRepository implements PassengerRepository {
  @Inject
  PassengerPanacheRepository repository;

  @Inject
  PassengerMapper userMapper;

  @Override
  public Passenger findById(PassengerId id) {
    return userMapper.toDomain(repository.findById(id.value()));
  }

  @Override
  public Passenger findByEmail(EmailAddress email) {
    return userMapper.toDomain(repository.find("email = ?1", email).firstResult());
  }

  @Override
  public Passenger findByExternalId(String externalId) {
    return userMapper.toDomain(repository.find("external_id = ?1", externalId).firstResult());
  }

  @Override
  public List<Passenger> getAll() {
    return repository.findAll().stream().map(p -> userMapper.toDomain(p)).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void save(Passenger user) {
    PassengerEntity entity = userMapper.toDbo(user);
    repository.persistAndFlush(entity);
    userMapper.updateDomainFromEntity(entity, user);
  }

  @Override
  @Transactional
  public void update(Passenger user) {
    PassengerEntity entity = repository.findByIdOptional(user.getId().value())
        .orElseThrow(() -> new DboException("No passenger found for admin Id [%s]", user.getId()));
    userMapper.updateEntityFromDomain(user, entity);
    repository.persist(entity);
    userMapper.updateDomainFromEntity(entity, user);
  }

  @Override
  @Transactional
  public void delete(String id) {
    repository.deleteById(id);
  }

}
