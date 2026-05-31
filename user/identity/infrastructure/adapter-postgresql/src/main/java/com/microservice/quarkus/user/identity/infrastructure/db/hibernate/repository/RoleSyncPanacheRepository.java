package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.identity.application.api.RoleSyncRepository;
import com.microservice.quarkus.user.identity.application.dto.RoleSyncRecord;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity.RoleSyncEntity;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.mapper.RoleSyncEntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleSyncPanacheRepository implements RoleSyncRepository {

  @Inject
  RoleSyncEntityMapper mapper;

  @Override
  public RoleSyncRecord findByName(String name) {
    RoleSyncEntity entity = RoleSyncEntity.find("name", name).firstResult();
    return entity != null ? mapper.toRecord(entity) : null;
  }

  @Override
  public List<RoleSyncRecord> findByClientId(String clientId) {
    return RoleSyncEntity.<RoleSyncEntity>list("clientId", clientId).stream()
        .map(mapper::toRecord)
        .collect(Collectors.toList());
  }

  @Override
  public RoleSyncRecord save(RoleSyncRecord record) {
    RoleSyncEntity managed = RoleSyncEntity.find("id", record.id()).firstResult();
    if (managed != null) {
      updateEntity(managed, record);
      return mapper.toRecord(managed);
    }
    RoleSyncEntity newEntity = mapper.toEntity(record);
    newEntity.persist();
    return mapper.toRecord(newEntity);
  }

  @Override
  public List<RoleSyncRecord> findAll() {
    return RoleSyncEntity.<RoleSyncEntity>listAll().stream()
        .map(mapper::toRecord)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteAll() {
    RoleSyncEntity.delete("from RoleSyncEntity", new java.util.HashMap<>());
  }

  private void updateEntity(RoleSyncEntity entity, RoleSyncRecord record) {
    entity.setName(record.name());
    entity.setDescription(record.description());
    entity.setSyncStatus(record.syncStatus());
    entity.setClientId(record.clientId());
    entity.setUpdatedAt(record.updatedAt());
  }
}
