package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.user.identity.application.dto.RoleSyncRecord;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity.RoleSyncEntity;

import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface RoleSyncEntityMapper {

  RoleSyncRecord toRecord(RoleSyncEntity entity);

  RoleSyncEntity toEntity(RoleSyncRecord record);
}
