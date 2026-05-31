package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity.UserSyncEntity;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.mapper.UserSyncEntityMapper;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserSyncPanacheRepository implements IdentitySyncRepository {

  @Inject
  UserSyncEntityMapper mapper;

  @Override
  public UserSyncRecord findByEmail(String email) {
    UserSyncEntity entity = UserSyncEntity.find("email", new EmailAddress(email)).firstResult();
    return entity != null ? mapper.toRecord(entity) : null;
  }

  @Override
  public UserSyncRecord findByExternalId(String externalId) {
    UserSyncEntity entity = UserSyncEntity.find("externalId", externalId).firstResult();
    return entity != null ? mapper.toRecord(entity) : null;
  }

  @Override
  public UserSyncRecord save(UserSyncRecord record) {
    UserSyncEntity managed = UserSyncEntity.find("id", record.id()).firstResult();
    if (managed != null) {
      updateEntity(managed, record);
      return mapper.toRecord(managed);
    }
    UserSyncEntity newEntity = mapper.toEntity(record);
    newEntity.persist();
    return mapper.toRecord(newEntity);
  }

  @Override
  public List<UserSyncRecord> findAll() {
    return UserSyncEntity.<UserSyncEntity>listAll().stream()
        .map(mapper::toRecord)
        .collect(Collectors.toList());
  }

  @Override
  public List<UserSyncRecord> findBySyncStatus(SyncStatus status) {
    return UserSyncEntity.find("syncStatus", status).<UserSyncEntity>stream()
        .map(mapper::toRecord)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteAll() {
    UserSyncEntity.delete("from UserSyncEntity", new java.util.HashMap<>());
  }

  @Override
  public void deleteByExternalId(String externalId) {
    UserSyncEntity.delete("externalId", externalId);
  }

  private void updateEntity(UserSyncEntity entity, UserSyncRecord record) {
    entity.setEmail(new EmailAddress(record.email()));
    entity.setExternalId(record.externalId());
    entity.setType(record.type());
    entity.setSyncStatus(record.syncStatus());
    entity.setUpdatedAt(record.updatedAt());
    entity.setRetryCount(record.retryCount());
    entity.setMaxRetries(record.maxRetries());
    entity.setNextRetryAt(record.nextRetryAt());
  }
}
