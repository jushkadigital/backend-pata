package com.microservice.quarkus.user.iam.infrastructure.keycloak.acl;

import com.fasterxml.jackson.databind.type.MapType;
import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;

import jakarta.inject.Named;

import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi") // Importante para inyección en Quarkus
public interface KeycloakRoleMapper {

  // Mapea el UserRepresentation (Keycloak) al objeto User (Dominio)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", expression = "java(entity.getName())")
  @Mapping(target = "description", expression = "java(entity.getDescription())")
  Role toDomain(RoleRepresentation entity);

}
