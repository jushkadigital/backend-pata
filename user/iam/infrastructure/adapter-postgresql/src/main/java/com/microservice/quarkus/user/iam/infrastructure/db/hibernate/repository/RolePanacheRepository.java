package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.repository;

import java.util.List;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.RoleEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolePanacheRepository implements PanacheRepositoryBase<RoleEntity, String> {

  public List<RoleEntity> findByClientId(String clientId) {
    return list("clientId", clientId);

  }

}
