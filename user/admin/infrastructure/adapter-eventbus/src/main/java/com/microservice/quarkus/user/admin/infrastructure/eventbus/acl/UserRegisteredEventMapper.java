package com.microservice.quarkus.user.admin.infrastructure.eventbus.acl;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserRegisteredEventMapper {

  @Mapping(source = "aggregateId", target = "externalId")
  CreateAdminCommand toCommand(UserRegisteredEventDTO dto);
}
