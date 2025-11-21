package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.MappingControl.Use;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.RoleEntity;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.UserEntity;

@Mapper(componentModel = "cdi")
public interface RoleMapper {

  @Mapping(source = "id", target = "id")
  RoleEntity toDbo(Role role);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "name", target = "name")
  Role toDomain(RoleEntity entity);

  void updateEntityFromDomain(Role role, @MappingTarget RoleEntity entity);

  @Mapping(target = "id", ignore = true)
  void updateDomainFromEntity(RoleEntity entity, @MappingTarget Role role);
}
