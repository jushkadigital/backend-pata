package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PassengerPanacheRepository implements PanacheRepositoryBase<PassengerEntity, String> {

}
