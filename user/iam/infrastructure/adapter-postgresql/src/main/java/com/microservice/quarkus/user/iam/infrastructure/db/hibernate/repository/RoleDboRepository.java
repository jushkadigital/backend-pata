package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.RoleRepository;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.domain.UserRepository;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.RoleEntity;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.exceptions.DboException;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.mapper.RoleMapper;

@ApplicationScoped
public class RoleDboRepository implements RoleRepository {
  @Inject
  RolePanacheRepository repository;

  @Inject
  RoleMapper userMapper;

  @Override
  public Role findById(String id) {
    return userMapper.toDomain(repository.findById(id));
  }

  @Override
  public Role findByName(String name) {
    return userMapper.toDomain(repository.find("name = ?1", name).firstResult());
  }

  @Override
  public List<Role> getAll() {
    return repository.findAll().stream().map(p -> userMapper.toDomain(p)).collect(Collectors.toList());
  }

  @Override
  public List<Role> getAllById(String clientId) {
    return repository.findByClientId(clientId).stream().map(p -> userMapper.toDomain(p)).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void save(Role user) {
    RoleEntity entity = userMapper.toDbo(user);
    repository.persistAndFlush(entity);
    userMapper.updateDomainFromEntity(entity, user);
  }

  @Override
  @Transactional
  public void update(Role user) {
    RoleEntity entity = repository.findByIdOptional(user.getId())
        .orElseThrow(() -> new DboException("No role found for role Id [%s]", user.getId()));
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
