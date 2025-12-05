package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.user.admin.domain.ports.out.AdminRepository;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.exceptions.DboException;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.mapper.AdminMapper;

@ApplicationScoped
public class AdminDboRepository implements AdminRepository {
  @Inject
  AdminPanacheRepository repository;

  @Inject
  AdminMapper userMapper;

  @Override
  public Admin findById(AdminId id) {
    return userMapper.toDomain(repository.findById(id.value()));
  }

  @Override
  public Admin findByEmail(EmailAddress email) {
    return userMapper.toDomain(repository.find("email = ?1", email).firstResult());
  }

  @Override
  public List<Admin> getAll() {
    return repository.findAll().stream().map(p -> userMapper.toDomain(p)).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void save(Admin user) {
    AdminEntity entity = userMapper.toDbo(user);
    repository.persistAndFlush(entity);
    userMapper.updateDomainFromEntity(entity, user);
  }

  @Override
  @Transactional
  public void update(Admin user) {
    AdminEntity entity = repository.findByIdOptional(user.getId().value())
        .orElseThrow(() -> new DboException("No admin found for admin Id [%s]", user.getId()));
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
