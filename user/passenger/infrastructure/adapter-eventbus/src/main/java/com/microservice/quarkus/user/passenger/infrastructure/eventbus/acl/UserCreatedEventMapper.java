package com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl;

import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserCreatedEventMapper {

  @Mapping(source = "aggregateId", target = "externalId")
  @Mapping(target = "passengerType", expression = "java(derivePassengerType(dto.clientRoles()))")
  CreatePassengerCommand toCommand(UserCreatedEventDTO dto);

  default String derivePassengerType(java.util.List<String> clientRoles) {
    if (clientRoles == null || clientRoles.isEmpty()) {
      return "STANDARD";
    }
    for (String role : clientRoles) {
      if ("premium".equalsIgnoreCase(role)) return "PREMIUM";
      if ("standard".equalsIgnoreCase(role)) return "STANDARD";
      if ("basic".equalsIgnoreCase(role)) return "BASIC";
    }
    return "STANDARD";
  }
}
