package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity.UserSyncEntity;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserSyncEntityMapper {

  @Mapping(target = "email", expression = "java(entity.getEmail().value())")
  @Mapping(target = "userType", source = "userType")
  @Mapping(target = "roles", expression = "java(rolesFromString(entity.getRoles()))")
  UserSyncRecord toRecord(UserSyncEntity entity);

  @Mapping(target = "email", expression = "java(new com.microservice.quarkus.user.shared.domain.EmailAddress(record.email()))")
  @Mapping(target = "userType", source = "userType")
  @Mapping(target = "roles", expression = "java(rolesToString(record.roles()))")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  @Mapping(target = "retryCount", source = "retryCount")
  @Mapping(target = "maxRetries", source = "maxRetries")
  @Mapping(target = "nextRetryAt", source = "nextRetryAt")
  UserSyncEntity toEntity(UserSyncRecord record);

  default List<String> rolesFromString(String roles) {
    if (roles == null || roles.isBlank()) {
      return List.of();
    }
    return Arrays.stream(roles.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  default String rolesToString(List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return "";
    }
    return roles.stream()
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(","));
  }
}
