package com.microservice.quarkus.catalog.tours.application.api;

import com.microservice.quarkus.catalog.tours.application.dto.AddPolicyToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.AddServiceToTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.CreateTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.PublishTourCommand;
import com.microservice.quarkus.catalog.tours.application.dto.SuspendTourCommand;
import com.microservice.quarkus.catalog.tours.domain.Tour;

import java.util.List;
import java.util.Optional;

public interface TourApiService {

    String create(CreateTourCommand cmd);

    void addService(AddServiceToTourCommand cmd);

    void addPolicy(AddPolicyToTourCommand cmd);

    void publish(PublishTourCommand cmd);

    void suspend(SuspendTourCommand cmd);

    void resume(String tourId);

    void discontinue(String tourId);

    Optional<Tour> findById(String tourId);

    Optional<Tour> findByCode(String code);

    List<Tour> findAll();

    List<Tour> findByStatus(String status);

    List<Tour> findPublished();

    List<Tour> findSellable();

    void delete(String tourId);
}
