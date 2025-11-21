package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.UserEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserPanacheRepository implements PanacheRepositoryBase<UserEntity, String> {

}
