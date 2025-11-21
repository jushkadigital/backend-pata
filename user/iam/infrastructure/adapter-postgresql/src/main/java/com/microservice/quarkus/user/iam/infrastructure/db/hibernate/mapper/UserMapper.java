package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.MappingControl.Use;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.UserEntity;

@Mapper(componentModel = "cdi")
public interface UserMapper {

  default String mapToString(EmailAddress value) {
    return value.value(); // ⬅️ Extrae el String del Record
  }

  // 2. String -> VO (Método de conversión para DTO -> Domain)
  default EmailAddress mapToEmail(String emailString) {
    return new EmailAddress(emailString);
  }

  default UserId mapToUserId(String value) {
    return value != null ? new UserId(value) : null;
  }

  default String mapToString(UserId value) {
    return value != null ? value.value() : null;
  }

  @Mapping(source = "id", target = "id")
  @Mapping(source = "email", target = "email")
  UserEntity toDbo(User domain);

  @Mapping(source = "id", target = "id")
  @Mapping(target = "email", source = "email")
  User toDomain(UserEntity entity);

  void updateEntityFromDomain(User domain, @MappingTarget UserEntity entity);

  @Mapping(target = "id", ignore = true)
  void updateDomainFromEntity(UserEntity entity, @MappingTarget User domain);
}
