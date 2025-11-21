package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.domain.UserRepository;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.UserEntity;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.exceptions.DboException;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.mapper.UserMapper;
import lombok.AllArgsConstructor;

@ApplicationScoped
public class UserDboRepository implements UserRepository {
  @Inject
  UserPanacheRepository repository;

  @Inject
  UserMapper userMapper;

  @Override
  public User findById(UserId id) {
    return userMapper.toDomain(repository.findById(id.value()));
  }

  @Override
  public User findByEmail(EmailAddress email) {
    return userMapper.toDomain(repository.find("email = ?1", email).firstResult());
  }

  @Override
  public List<User> getAll() {
    return repository.findAll().stream().map(p -> userMapper.toDomain(p)).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void save(User user) {
    UserEntity entity = userMapper.toDbo(user);
    repository.persistAndFlush(entity);
    userMapper.updateDomainFromEntity(entity, user);
  }

  @Override
  @Transactional
  public void update(User user) {
    UserEntity entity = repository.findByIdOptional(user.getId().value())
        .orElseThrow(() -> new DboException("No user found for user Id [%s]", user.getId()));
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
