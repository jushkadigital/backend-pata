package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.mapper;

import com.microservice.quarkus.user.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.OutboxEventEntity;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface OutboxEventMapper {

  OutboxEvent toDomain(OutboxEventEntity entity);

  OutboxEventEntity toDbo(OutboxEvent domain);

  void updateEntityFromDomain(OutboxEvent domain, @MappingTarget OutboxEventEntity entity);

  void updateDomainFromEntity(OutboxEventEntity entity, @MappingTarget OutboxEvent domain);
}
