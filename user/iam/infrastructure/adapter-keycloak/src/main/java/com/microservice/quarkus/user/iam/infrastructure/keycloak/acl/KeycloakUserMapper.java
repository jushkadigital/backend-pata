package com.microservice.quarkus.user.iam.infrastructure.keycloak.acl;

import com.fasterxml.jackson.databind.type.MapType;
import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;

import jakarta.inject.Named;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi") // Importante para inyección en Quarkus
public interface KeycloakUserMapper {

  // Mapea el UserRepresentation (Keycloak) al objeto User (Dominio)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "externalId", expression = "java(entity.getId())")
  @Mapping(target = "email", expression = "java(new com.microservice.quarkus.user.iam.domain.EmailAddress(entity.getEmail()))")
  User toDomain(UserRepresentation entity);

}
