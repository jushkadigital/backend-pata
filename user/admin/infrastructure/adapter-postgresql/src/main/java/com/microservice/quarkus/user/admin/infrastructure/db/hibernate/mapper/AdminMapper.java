package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.MappingControl.Use;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;

@Mapper(componentModel = "cdi")
public interface AdminMapper {

  default String mapToString(EmailAddress value) {
    return value.value(); // ⬅️ Extrae el String del Record
  }

  // 2. String -> VO (Método de conversión para DTO -> Domain)
  default EmailAddress mapToEmail(String emailString) {
    return new EmailAddress(emailString);
  }

  default AdminId mapToUserId(String value) {
    return value != null ? new AdminId(value) : null;
  }

  default String mapToString(AdminId value) {
    return value != null ? value.value() : null;
  }

  @Mapping(source = "id", target = "id")
  @Mapping(source = "email", target = "email")
  AdminEntity toDbo(Admin domain);

  @Mapping(source = "id", target = "id")
  @Mapping(target = "email", source = "email")
  Admin toDomain(AdminEntity entity);

  void updateEntityFromDomain(Admin domain, @MappingTarget AdminEntity entity);

  @Mapping(target = "id", ignore = true)
  void updateDomainFromEntity(AdminEntity entity, @MappingTarget Admin domain);

}
