package com.microservice.quarkus.payment.infrastructure.postgresql.outbox;

import com.microservice.quarkus.payment.domain.outbox.OutboxEvent;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface OutboxEventMapper {

    OutboxEvent toDomain(OutboxEventEntity entity);

    OutboxEventEntity toDbo(OutboxEvent domain);

    void updateEntityFromDomain(OutboxEvent domain, @MappingTarget OutboxEventEntity entity);

    void updateDomainFromEntity(OutboxEventEntity entity, @MappingTarget OutboxEvent domain);
}
