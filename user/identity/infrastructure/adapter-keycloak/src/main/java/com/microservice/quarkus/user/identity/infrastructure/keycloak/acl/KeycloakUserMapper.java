package com.microservice.quarkus.user.identity.infrastructure.keycloak.acl;

import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface KeycloakUserMapper {

  @Mapping(target = "id", expression = "java(entity.getId())")
  @Mapping(target = "email", expression = "java(entity.getEmail())")
  @Mapping(target = "externalId", expression = "java(entity.getId())")
  @Mapping(target = "type", ignore = true)
  KeycloakUserDTO toDTO(UserRepresentation entity);

}
