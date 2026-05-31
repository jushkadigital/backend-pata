package com.microservice.quarkus.user.identity.infrastructure.keycloak.acl;

import com.microservice.quarkus.user.identity.application.dto.KeycloakRoleDTO;

import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface KeycloakRoleMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", expression = "java(entity.getName())")
  @Mapping(target = "description", expression = "java(entity.getDescription())")
  @Mapping(target = "clientId", ignore = true)
  KeycloakRoleDTO toDTO(RoleRepresentation entity);

}
