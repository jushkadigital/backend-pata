package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity.UserSyncEntity;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserSyncEntityMapper {

  @Mapping(target = "email", expression = "java(entity.getEmail().value())")
  UserSyncRecord toRecord(UserSyncEntity entity);

  @Mapping(target = "email", expression = "java(new com.microservice.quarkus.user.shared.domain.EmailAddress(record.email()))")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  @Mapping(target = "retryCount", source = "retryCount")
  @Mapping(target = "maxRetries", source = "maxRetries")
  @Mapping(target = "nextRetryAt", source = "nextRetryAt")
  UserSyncEntity toEntity(UserSyncRecord record);
}
