package com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl;

import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserRegisteredEventMapper {

  @Mapping(source = "aggregateId", target = "externalId")
  @Mapping(target = "type", source = "type")
  @Mapping(target = "subType", source = "subType")
  CreatePassengerCommand toCommand(UserRegisteredEventDTO dto);
}
