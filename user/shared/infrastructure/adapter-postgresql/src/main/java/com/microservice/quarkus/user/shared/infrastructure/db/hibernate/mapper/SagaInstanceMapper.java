package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.user.shared.application.saga.SagaInstance;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.SagaInstanceEntity;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface SagaInstanceMapper {

  SagaInstance toDomain(SagaInstanceEntity entity);

  SagaInstanceEntity toEntity(SagaInstance domain);

  void updateEntityFromDomain(SagaInstance domain, @MappingTarget SagaInstanceEntity entity);
}
