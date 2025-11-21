package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdminPanacheRepository implements PanacheRepositoryBase<AdminEntity, String> {

}
